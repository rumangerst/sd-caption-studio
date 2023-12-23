package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import de.mrnotsoevil.sdcaptionstudio.api.events.*;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import ij.IJ;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Disposable;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class SDCaptionProject implements Disposable {

    private final SDCaptionedImagePropertyUpdatedEventEmitter captionedImagePropertyUpdatedEventEmitter
            = new SDCaptionedImagePropertyUpdatedEventEmitter();
    private final SDCaptionProjectReloadedEventEmitter projectReloadedEventEmitter
            = new SDCaptionProjectReloadedEventEmitter();
    private final SDCaptionProjectTemplatesChangedEventEmitter projectTemplatesChangedEventEmitter
            = new SDCaptionProjectTemplatesChangedEventEmitter();
    private final SDCaptionProjectTagsChangedEventEmitter projectTagsChangedEventEmitter
            = new SDCaptionProjectTagsChangedEventEmitter();
    private Map<String, SDCaptionedImage> images = new HashMap<>();
    private Path storagePath;
    private Path workDirectory;
    private Path projectFilePath;
    private Map<String, SDCaptionTemplate> templates = new HashMap<>();
    private List<SDCaptionAutocompleteTag> tags = new ArrayList<>();

    private boolean saveCaptionsOnProjectSave = true;

    public SDCaptionProject() {
    }

    public static SDCaptionProject loadProject(Path file) {
        SDCaptionProject project = new SDCaptionProject();
        project.setProjectFilePath(file);
        project.setWorkDirectory(file.getParent());
        project.reload();
        return project;
    }

    public Path getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
    }

    public Path getProjectFilePath() {
        return projectFilePath;
    }

    public void setProjectFilePath(Path projectFilePath) {
        this.projectFilePath = projectFilePath;
    }

    @JsonGetter("storage-path")
    public Path getStoragePath() {
        return storagePath;
    }

    @JsonSetter("storage-path")
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    @JsonGetter("save-captions-on-project-save")
    public boolean isSaveCaptionsOnProjectSave() {
        return saveCaptionsOnProjectSave;
    }

    @JsonSetter("save-captions-on-project-save")
    public void setSaveCaptionsOnProjectSave(boolean saveCaptionsOnProjectSave) {
        this.saveCaptionsOnProjectSave = saveCaptionsOnProjectSave;
    }

    @JsonGetter("templates")
    public Map<String, SDCaptionTemplate> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    @JsonSetter("templates")
    public void setTemplates(Map<String, SDCaptionTemplate> templates) {
        this.templates = templates;
        for (SDCaptionTemplate template : templates.values()) {
            template.setProject(this);
        }
        projectTemplatesChangedEventEmitter.emit(new SDCaptionProjectTemplatesChangedEvent(this));
    }

    @JsonGetter("tags")
    public List<SDCaptionAutocompleteTag> getTags() {
        return tags;
    }

    @JsonSetter("tags")
    public void setTags(List<SDCaptionAutocompleteTag> tags) {
        this.tags = tags;
        projectTagsChangedEventEmitter.emit(new SDCaptionProjectTagsChangedEvent(this));
        for (SDCaptionAutocompleteTag tag : tags) {
            tag.setProject(this);
        }
    }

    public SDCaptionTemplate createTemplate(String key, String content) {
        key = SDCaptionUtils.toValidTemplateKey(key);
        if(!templates.containsKey(key)) {
            SDCaptionTemplate template = new SDCaptionTemplate();
            template.setProject(this);
            template.setContent(content);
            templates.put(key, template);
            projectTemplatesChangedEventEmitter.emit(new SDCaptionProjectTemplatesChangedEvent(this));
            return template;
        }
        return null;
    }

    public void removeTemplate(String key) {
        if (templates.containsKey(key)) {
            String content = templates.get(key).getContent();
            templates.remove(key);
            projectTemplatesChangedEventEmitter.emit(new SDCaptionProjectTemplatesChangedEvent(this));

            // Expand any editing caption
            for (SDCaptionedImage image : images.values()) {
                image.setUserCaption(compileCaption(image.getUserCaption(), key, content, true));
            }

        }
    }

    public Path getAbsoluteStoragePath() {
        if (storagePath.isAbsolute()) {
            return storagePath;
        } else {
            return workDirectory.resolve(storagePath);
        }
    }

    public void relativizeStoragePath() {
        if (storagePath.isAbsolute()) {
            storagePath = workDirectory.relativize(storagePath);
        }
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();
    }

    @JsonGetter("images")
    public Map<String, SDCaptionedImage> getImages() {
        return Collections.unmodifiableMap(images);
    }

    @JsonSetter("images")
    public void setImages(Map<String, SDCaptionedImage> images) {
        this.images = images;
    }

    public void reload() {

        // Load the project file if present
        try {
            if(Files.isRegularFile(projectFilePath)) {
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(projectFilePath.toFile(), JsonNode.class);
                JsonUtils.getObjectMapper().readerForUpdating(this).readValue(jsonData);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Detect new images/load captions for them
        final FileNameExtensionFilter[] filters = new FileNameExtensionFilter[]{
                UIUtils.EXTENSION_FILTER_PNG,
                UIUtils.EXTENSION_FILTER_BMP,
                UIUtils.EXTENSION_FILTER_JPEG,
                UIUtils.EXTENSION_FILTER_TIFF
        };
        Path absoluteStoragePath = getAbsoluteStoragePath();
        try (Stream<Path> stream = Files.walk(absoluteStoragePath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach(path -> {
                if (Files.isRegularFile(path) && Arrays.stream(filters).anyMatch(predicate -> predicate.accept(path.toFile()))) {
                    String name = absoluteStoragePath.relativize(path).toString();
                    name = name.substring(0, name.lastIndexOf('.'));

                    SDCaptionedImage image = images.getOrDefault(name, null);
                    if (image == null) {
                        image = new SDCaptionedImage();
                        images.put(name, image);
                    }

                    image.setProject(this);
                    image.setName(name);
                    image.setImagePath(path);
                    image.setCaptionPath(absoluteStoragePath.resolve(name + ".txt"));

                    image.loadCaptionFromFile(false);
                }
            });
        } catch (Exception e) {
            IJ.handleException(e);
        } finally {
            projectReloadedEventEmitter.emit(new SDCaptionProjectReloadedEvent(this));
        }
    }

    public void saveProject(Path file) throws IOException {
        relativizeStoragePath();

        // Commit all changes
        if (saveCaptionsOnProjectSave) {
            for (SDCaptionedImage value : images.values()) {
                if (value.isUserCaptionEdited()) {
                    value.saveCaptionToFile();
                }
            }
        }

        JsonUtils.saveToFile(this, file);
    }

    public SDCaptionedImagePropertyUpdatedEventEmitter getCaptionedImagePropertyUpdatedEventEmitter() {
        return captionedImagePropertyUpdatedEventEmitter;
    }

    public SDCaptionProjectTemplatesChangedEventEmitter getProjectTemplatesChangedEventEmitter() {
        return projectTemplatesChangedEventEmitter;
    }

    public SDCaptionProjectTagsChangedEventEmitter getProjectTagsChangedEventEmitter() {
        return projectTagsChangedEventEmitter;
    }

    public SDCaptionProjectReloadedEventEmitter getProjectReloadedEventEmitter() {
        return projectReloadedEventEmitter;
    }

    public String compileCaption(String caption) {
        for (Map.Entry<String, SDCaptionTemplate> entry : templates.entrySet()) {
            caption = compileCaption(caption, entry.getKey(), entry.getValue().getContent(), true);
        }
        return caption;
    }

    public String decompileCaption(String caption, String key, String content) {
        if(!caption.startsWith(" ")) {
            caption = " " + caption;
        }
        if(!caption.endsWith(" ")) {
            caption = caption + " ";
        }
        content = compileCaption(content);
        for (char c1 : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
            for (char c2 : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
                caption = caption.replace(c1 + content + c2, c1 + "@" + key + c2);
            }
        }
        return caption;
    }

    public String compileCaption(String caption, String key, String content, boolean resolveNestedTemplates) {
        if (!caption.endsWith(" ")) {
            caption = caption + " ";
        }

        // expand templates in nested content
        if(resolveNestedTemplates) {
            content = content.trim();
            Set<String> visited = new HashSet<>();
            visited.add(key);
            while (content.contains("@")) {
                boolean changed = false;
                for (Map.Entry<String, SDCaptionTemplate> entry : templates.entrySet()) {
                    if(visited.contains(entry.getKey())) {
                        continue;
                    }
                    visited.add(entry.getKey());
                    String newContent = compileCaption(caption, entry.getKey(), entry.getValue().getContent(), false);
                    if(!newContent.equals(content)) {
                        changed = true;
                        content = newContent;
                    }
                }
                if(!changed) {
                    break;
                }
            }
        }

        String variable = SDCaptionUtils.toValidTemplateKey(key);
        if (StringUtils.isNullOrEmpty(variable)) {
            return caption;
        }
        for (char c : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
            caption = caption.replace("@" + variable + c, content + c);
        }
        return caption.trim();
    }

    public String findTemplateKey(SDCaptionTemplate template) {
        for (Map.Entry<String, SDCaptionTemplate> entry : templates.entrySet()) {
            if(Objects.equals(entry.getValue().getContent(), template.getContent())) {
                return entry.getKey();
            }
        }

        return null;
    }

    public void addTemplate(SDCaptionTemplate template, boolean allowOverwrite) {
        String validKey = SDCaptionUtils.toValidTemplateKey(template.getKey());
        if(!StringUtils.isNullOrEmpty(validKey)) {
            if(!allowOverwrite) {
                validKey = StringUtils.makeUniqueString(validKey, "_", templates.keySet());
            }
            templates.remove(validKey);
            SDCaptionTemplate template1 = createTemplate(validKey, template.getContent());
            template1.copyMetadataFrom(template);
            projectTemplatesChangedEventEmitter.emit(new SDCaptionProjectTemplatesChangedEvent(this));
        }
    }

    public void duplicateImage(String name, String newName) {
        SDCaptionedImage image = images.get(name);
        if(image == null) {
            return;
        }
        if(newName == null || images.containsKey(newName)) {
            newName = StringUtils.makeUniqueString(newName, "_", images.keySet());
        }
        String imageExtension = image.getImagePath().getFileName().toString();
        imageExtension = imageExtension.substring(imageExtension.lastIndexOf('.'));
        Path newImageFile = getWorkDirectory().resolve(image.getImagePath().getParent()).resolve(newName + imageExtension);
        Path newCaptionFile = getWorkDirectory().resolve(image.getCaptionPath().getParent()).resolve(newName + ".txt");
        try {
            Files.copy(image.getImagePath(), newImageFile);
            if(Files.isRegularFile(image.getCaptionPath())) {
                Files.copy(image.getCaptionPath(), newCaptionFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SDCaptionedImage newCaptionedImage = new SDCaptionedImage(image);
        newCaptionedImage.setName(newName);
        newCaptionedImage.setImagePath(newImageFile);
        newCaptionedImage.setCaptionPath(newCaptionFile);
        images.put(newName, newCaptionedImage);
        projectReloadedEventEmitter.emit(new SDCaptionProjectReloadedEvent(this));
    }

    public void deleteImage(String name) {
        SDCaptionedImage image = images.get(name);
        if(image == null) {
            return;
        }
        try {
            if(Files.isRegularFile(image.getImagePath())) {
                Files.delete(image.getImagePath());
            }
            if(Files.isRegularFile(image.getCaptionPath())) {
                Files.delete(image.getCaptionPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        images.remove(name);
        projectReloadedEventEmitter.emit(new SDCaptionProjectReloadedEvent(this));
    }

    public void renameImage(String oldName, String newName) {
        SDCaptionedImage image = images.get(oldName);
        if(image == null) {
            return;
        }
        if(newName == null || images.containsKey(newName)) {
            newName = StringUtils.makeUniqueString(newName, "_", images.keySet());
        }
        String imageExtension = image.getImagePath().getFileName().toString();
        imageExtension = imageExtension.substring(imageExtension.lastIndexOf('.'));
        Path newImageFile = getWorkDirectory().resolve(image.getImagePath().getParent()).resolve(newName + imageExtension);
        Path newCaptionFile = getWorkDirectory().resolve(image.getCaptionPath().getParent()).resolve(newName + ".txt");
        try {
            // Copy to new name
            Files.copy(image.getImagePath(), newImageFile);
            if(Files.isRegularFile(image.getCaptionPath())) {
                Files.copy(image.getCaptionPath(), newCaptionFile);
            }
            // Remove old file
            if(Files.isRegularFile(image.getImagePath())) {
                Files.delete(image.getImagePath());
            }
            if(Files.isRegularFile(image.getCaptionPath())) {
                Files.delete(image.getCaptionPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        image.setName(newName);
        image.setImagePath(newImageFile);
        image.setCaptionPath(newCaptionFile);
        images.remove(oldName);
        images.put(newName, image);
        projectReloadedEventEmitter.emit(new SDCaptionProjectReloadedEvent(this));
    }

    public List<SDCaptionedImage> getSortedImages() {
        List<SDCaptionedImage> images = new ArrayList<>(getImages().values());
        images.sort(Comparator.comparing(SDCaptionedImage::getName, NaturalOrderComparator.INSTANCE));
        return images;
    }

    public int importTagsFromString(String csvString, String source) {
        Set<String> usedTags = new HashSet<>();
        for (SDCaptionAutocompleteTag tag : tags) {
            usedTags.add(tag.getKey());
        }


        int count = 0;
        // First try the auto1111 plugin's format
        try {
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withIgnoreQuotations(true)
                    .build();

            CSVReader csvReader = new CSVReaderBuilder(new StringReader(csvString))
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if(line.length == 1) {
                    throw new IllegalArgumentException("Invalid format!");
                }
                String tag = line[0].trim();
                String alternatives = line.length > 3 ? line[3] : "";

                if(!tag.isEmpty()) {
                    // Standard case 1
                    if(!usedTags.contains(tag)) {
                        SDCaptionAutocompleteTag instance = new SDCaptionAutocompleteTag();
                        instance.setKey(tag);
                        instance.setSource(source);
                        instance.setProject(this);

                        // Add and register
                        tags.add(instance);
                        usedTags.add(tag);
                        ++count;
                    }

                    // Resolve alternatives
                    if(!alternatives.isEmpty()) {
                        for (String alternative : alternatives.split(",")) {
                            String trimmed = alternative.trim();
                            if(!usedTags.contains(trimmed)) {
                                SDCaptionAutocompleteTag alternativeInstance = new SDCaptionAutocompleteTag();
                                alternativeInstance.setKey(trimmed);
                                alternativeInstance.setReplacement(tag);
                                alternativeInstance.setSource(source);
                                alternativeInstance.setProject(this);

                                tags.add(alternativeInstance);
                                usedTags.add(trimmed);
                                ++count;
                            }
                        }
                    }
                }
                else if(!alternatives.isEmpty()) {
                    // Weird case where alternatives are mapped to empty?
                    // We just add them as tag
                    for (String alternative : alternatives.split(",")) {
                        String trimmed = alternative.trim();
                        if(!usedTags.contains(trimmed)) {
                            SDCaptionAutocompleteTag alternativeInstance = new SDCaptionAutocompleteTag();
                            alternativeInstance.setKey(trimmed);
                            alternativeInstance.setSource(source);
                            alternativeInstance.setProject(this);

                            tags.add(alternativeInstance);
                            usedTags.add(trimmed);
                            ++count;
                        }
                    }
                }
            }
        } catch (CsvValidationException | IOException e) {
            throw new RuntimeException(e);
        }

        projectTagsChangedEventEmitter.emit(new SDCaptionProjectTagsChangedEvent(this));
        return count;
    }

    public int importTagsFromFile(Path csvPath) {
        try {
            return importTagsFromString(new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8), csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeTags(List<SDCaptionAutocompleteTag> toRemove) {
        tags.removeAll(toRemove);
        projectTagsChangedEventEmitter.emit(new SDCaptionProjectTagsChangedEvent(this));
    }

    public void clearTags() {
        tags.clear();
        projectTagsChangedEventEmitter.emit(new SDCaptionProjectTagsChangedEvent(this));
    }
}
