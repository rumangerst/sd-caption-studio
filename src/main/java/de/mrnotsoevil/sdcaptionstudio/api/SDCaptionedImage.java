package de.mrnotsoevil.sdcaptionstudio.api;

import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImageInfoUpdatedEvent;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Path;

public class SDCaptionedImage {

    private SDCaptionProject project;
    private String name;
    private Path imagePath;
    private Path captionPath;
    private String caption = "";
    private int numTokens;
    private boolean captionEdited;
    private SDCaptionedImageInfo currentImageInfo;

    private SDCaptionedImageInfoLoader imageInfoLoader;

    public SDCaptionedImageInfo getCurrentImageInfo() {
        return currentImageInfo;
    }

    public void setCurrentImageInfo(SDCaptionedImageInfo currentImageInfo) {
        this.currentImageInfo = currentImageInfo;
        if(project != null) {
            project.getCaptionedImageInfoUpdatedEventEmitter().emit(new SDCaptionedImageInfoUpdatedEvent(this));
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = StringUtils.nullToEmpty(caption);
        this.numTokens = this.caption.split("\\s+").length;
    }

    public int getNumTokens() {
        return numTokens;
    }

    public boolean isCaptionEdited() {
        return captionEdited;
    }

    public void setCaptionEdited(boolean captionEdited) {
        this.captionEdited = captionEdited;
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
}
