package de.mrnotsoevil.sdcaptionstudio.api;

import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class SDCaptionedImage {

    private SDCaptionProject project;
    private String name;
    private Path imagePath;
    private Path captionPath;
    private String savedCaption = "";
    private String editedCaption = null;
    private int numTokens;
    private SDCaptionedImageInfo currentImageInfo;

    private SDCaptionedImageInfoLoader imageInfoLoader;

    public SDCaptionedImageInfo getCurrentImageInfo() {
        return currentImageInfo;
    }

    public void setCurrentImageInfo(SDCaptionedImageInfo currentImageInfo) {
        this.currentImageInfo = currentImageInfo;
        if(project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.ImageInfo));
        }
    }

    public SDCaptionedImageInfo getImageInfoForUI() {
        if(currentImageInfo != null) {
            return currentImageInfo;
        }
        else {
            synchronized (this) {
                if(imageInfoLoader == null) {
                    imageInfoLoader = new SDCaptionedImageInfoLoader(this, 64);
                    imageInfoLoader.execute();
                }
                SDCaptionedImageInfo info = new SDCaptionedImageInfo();
                info.setSize("Loading ...");
                info.setThumbnail(UIUtils.getIcon16FromResources("actions/hourglass-half.png"));
                return info;
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getImagePath() {
        return imagePath;
    }

    public void setImagePath(Path imagePath) {
        this.imagePath = imagePath;
    }

    public String getSavedCaption() {
        return savedCaption;
    }

    public void setSavedCaption(String savedCaption) {
        this.savedCaption = StringUtils.nullToEmpty(savedCaption);
        this.editedCaption = null;
        updateCaptionTokens();

        if(project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.Caption));
        }
    }

    private void updateCaptionTokens() {
        if(!StringUtils.isNullOrEmpty(getFinalCaption())) {
            this.numTokens = getFinalCaption().split("\\s+").length;
        }
        else {
            this.numTokens = 0;
        }
    }

    public String getEditedCaption() {
        return editedCaption;
    }

    public void setEditedCaption(String editedCaption) {
        this.editedCaption = editedCaption;
        updateCaptionTokens();

        if(project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.Caption));
        }
    }

    public void resetCaption() {
        setEditedCaption(null);
    }

    public String getFinalCaption() {
       if(project != null) {
           return project.captionExpandTemplates(getRawCaption());
       }
       else {
           return getRawCaption();
       }
    }

    public String getRawCaption() {
        if(editedCaption != null) {
            return StringUtils.nullToEmpty(editedCaption);
        }
        else {
            return StringUtils.nullToEmpty(savedCaption);
        }
    }

    public String getShortenedCaption() {
        if(project != null) {
            return project.captionContractTemplates(getRawCaption());
        }
        else {
            return getRawCaption();
        }
    }

    public boolean isCaptionEdited() {
        return editedCaption != null && !Objects.equals(savedCaption, editedCaption);
    }

    public int getNumTokens() {
        return numTokens;
    }

    public Path getCaptionPath() {
        return captionPath;
    }

    public void setCaptionPath(Path captionPath) {
        this.captionPath = captionPath;
    }

    public SDCaptionProject getProject() {
        return project;
    }

    public void setProject(SDCaptionProject project) {
        this.project = project;
    }

    public void saveCaption() throws IOException {
        if(isCaptionEdited()) {
           savedCaption = getEditedCaption();
           setEditedCaption(null);
        }
        Files.write(getCaptionPath(), getFinalCaption().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }
}
