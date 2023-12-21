package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import de.mrnotsoevil.sdcaptionstudio.api.events.*;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import ij.IJ;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Disposable;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SDCaptionProject implements Disposable {

    private final SDCaptionedImagePropertyUpdatedEventEmitter captionedImagePropertyUpdatedEventEmitter
            = new SDCaptionedImagePropertyUpdatedEventEmitter();
    private final SDCaptionProjectReloadedEventEmitter projectReloadedEventEmitter
            = new SDCaptionProjectReloadedEventEmitter();
    private final SDCaptionProjectTemplatesChangedEventEmitter projectTemplatesChangedEventEmitter
            = new SDCaptionProjectTemplatesChangedEventEmitter();
    private final Map<String, SDCaptionedImage> images = new HashMap<>();
    private Path storagePath;
    private Path workDirectory;
    private Path projectFilePath;
    private Map<String, SDCaptionTemplate> templates = new HashMap<>();

    private boolean saveCaptionsOnProjectSave = true;

    public SDCaptionProject() {
    }

    public static SDCaptionProject loadProject(Path file) {
        SDCaptionProject project = new SDCaptionProject();
        project.setProjectFilePath(file);
        project.setWorkDirectory(file.getParent());
        try {
            JsonUtils.getObjectMapper().readerForUpdating(project).readValue(file.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                if(image.isCaptionEdited()) {
                    image.setEditedCaption(captionExpandTemplate(image.getShortenedCaption(), key, content, true));
                }
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

    public Map<String, SDCaptionedImage> getImages() {
        return Collections.unmodifiableMap(images);
    }

    public void reset() {
        images.clear();
        reload();
        // TODO: handle reload
    }

    public void reload() {
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

                    // Load caption if not edited by user
                    if (!image.isCaptionEdited()) {
                        if (Files.isRegularFile(image.getCaptionPath())) {
                            try {
                                image.setSavedCaption(new String(Files.readAllBytes(image.getCaptionPath()), StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                IJ.handleException(e);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            IJ.handleException(e);
        } finally {
            projectReloadedEventEmitter.emit(new SDCaptionProjectReloadedEvent(this));
        }
    }

    public void readMetadataFromJson(JsonNode jsonData) throws IOException {
        JsonUtils.getObjectMapper().readerForUpdating(this).readValue(jsonData);
    }

    public void saveProject(Path file) throws IOException {
        relativizeStoragePath();
        JsonUtils.saveToFile(this, file);

        // Commit all changes
        if (saveCaptionsOnProjectSave) {
            for (SDCaptionedImage value : images.values()) {
                if (value.isCaptionEdited()) {
                    value.saveCaption();
                }
            }
        }
    }

    public SDCaptionedImagePropertyUpdatedEventEmitter getCaptionedImagePropertyUpdatedEventEmitter() {
        return captionedImagePropertyUpdatedEventEmitter;
    }

    public SDCaptionProjectTemplatesChangedEventEmitter getProjectTemplatesChangedEventEmitter() {
        return projectTemplatesChangedEventEmitter;
    }

    public SDCaptionProjectReloadedEventEmitter getProjectReloadedEventEmitter() {
        return projectReloadedEventEmitter;
    }



    public String captionContractTemplates(String caption) {
        List<Map.Entry<String, SDCaptionTemplate>> orderedEntries = templates.entrySet().stream().sorted(
                Comparator.comparing((Map.Entry<String, SDCaptionTemplate> entry) ->
                entry.getValue().getContent().length())).collect(Collectors.toList());
        orderedEntries = Lists.reverse(orderedEntries);

        // Contract from the largest to the smallest with spacing
        caption = " " + caption + " "; // Safety-spacing

        for (Map.Entry<String, SDCaptionTemplate> entry : orderedEntries) {
            for (char c : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
                caption = caption.replace(c + entry.getValue().getContent() + c, c + "@" + SDCaptionUtils.toValidTemplateKey(entry.getKey()) + c);
            }
        }

        return caption;
    }

    public String captionExpandTemplates(String caption) {
        for (Map.Entry<String, SDCaptionTemplate> entry : templates.entrySet()) {
            caption = captionExpandTemplate(caption, entry.getKey(), entry.getValue().getContent(), true);
        }
        return caption;
    }

    private String captionExpandTemplate(String caption, String key, String content, boolean resolveNestedTemplates) {
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
                    String newContent = captionExpandTemplate(caption, entry.getKey(), entry.getValue().getContent(), false);
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
}
