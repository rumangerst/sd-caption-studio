package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEventEmitter;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEventEmitter;
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

    private final SDCaptionedImagePropertyUpdatedEventEmitter captionedImageInfoUpdatedEventEmitter = new SDCaptionedImagePropertyUpdatedEventEmitter();
    private final SDCaptionProjectReloadedEventEmitter projectReloadedEventEmitter = new SDCaptionProjectReloadedEventEmitter();
    private Path storagePath;
    private Path workDirectory;
    private Path projectFilePath;
    private final Map<String, SDCaptionedImage> images = new HashMap<>();
    private Map<String, SDCaptionTemplate> templates = new HashMap<>();

    private boolean saveCaptionsOnProjectSave = true;

    public SDCaptionProject() {
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
    @JsonSetter("storage-path")
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    @JsonGetter("storage-path")
    public Path getStoragePath() {
        return storagePath;
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
        return templates;
    }

    @JsonSetter("templates")
    public void setTemplates(Map<String, SDCaptionTemplate> templates) {
        this.templates = templates;
    }

    public Path getAbsoluteStoragePath() {
        if(storagePath.isAbsolute()) {
            return storagePath;
        }
        else {
            return workDirectory.resolve(storagePath);
        }
    }

    public void relativizeStoragePath() {
        if(storagePath.isAbsolute()) {
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
        final FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
          UIUtils.EXTENSION_FILTER_PNG,
          UIUtils.EXTENSION_FILTER_BMP,
          UIUtils.EXTENSION_FILTER_JPEG,
          UIUtils.EXTENSION_FILTER_TIFF
        };
        Path absoluteStoragePath = getAbsoluteStoragePath();
        try(Stream<Path> stream = Files.walk(absoluteStoragePath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach(path -> {
                if(Files.isRegularFile(path) && Arrays.stream(filters).anyMatch(predicate -> predicate.accept(path.toFile()))) {
                    String name = absoluteStoragePath.relativize(path).toString();
                    name = name.substring(0, name.lastIndexOf('.'));

                    SDCaptionedImage image = images.getOrDefault(name, null);
                    if(image == null) {
                        image = new SDCaptionedImage();
                        images.put(name, image);
                    }

                    image.setProject(this);
                    image.setName(name);
                    image.setImagePath(path);
                    image.setCaptionPath(absoluteStoragePath.resolve(name + ".txt"));

                    // Load caption if not edited by user
                    if(!image.isCaptionEdited()) {
                        if(Files.isRegularFile(image.getCaptionPath())) {
                            try {
                                image.setSavedCaption(new String(Files.readAllBytes(image.getCaptionPath()), StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                IJ.handleException(e);
                            }
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            IJ.handleException(e);
        }
        finally {
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
        if(saveCaptionsOnProjectSave) {
            for (SDCaptionedImage value : images.values()) {
                if(value.isCaptionEdited()) {
                    value.saveCaption();
                }
            }
        }
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

    public SDCaptionedImagePropertyUpdatedEventEmitter getCaptionedImagePropertyUpdatedEventEmitter() {
        return captionedImageInfoUpdatedEventEmitter;
    }

    public SDCaptionProjectReloadedEventEmitter getProjectReloadedEventEmitter() {
        return projectReloadedEventEmitter;
    }

    public String captionContractTemplates(String caption) {
        List<Map.Entry<String, SDCaptionTemplate>> orderedEntries = templates.entrySet().stream().sorted(Comparator.comparing((Map.Entry<String, SDCaptionTemplate> entry) ->
                entry.getValue().getContent().length())).collect(Collectors.toList());
        orderedEntries = Lists.reverse(orderedEntries);

        // Contract from the largest to the smallest with spacing
        caption = " " + caption + " "; // Safety-spacing

        for (Map.Entry<String, SDCaptionTemplate> entry : orderedEntries) {
            for(char c : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
                caption = caption.replace(c + entry.getValue().getContent() + c, "@" + SDCaptionUtils.toValidTemplateKey(entry.getKey()) + c);
            }
        }

        return caption;
    }

    public String captionExpandTemplates(String caption) {
        if(!caption.endsWith(" ")) {
            caption = caption + " ";
        }
        for (Map.Entry<String, SDCaptionTemplate> entry : templates.entrySet()) {
            String variable = SDCaptionUtils.toValidTemplateKey(entry.getKey());
            if(StringUtils.isNullOrEmpty(variable)) {
                continue;
            }
            for(char c : SDCaptionUtils.TEMPLATE_SEPARATOR_CHARS) {
                caption = caption.replace("@" + variable + c, entry.getValue().getContent() + c);
            }
        }
        return caption.trim();
    }

}
