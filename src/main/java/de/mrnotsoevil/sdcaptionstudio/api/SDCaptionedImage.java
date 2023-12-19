package de.mrnotsoevil.sdcaptionstudio.api;

import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

public class SDCaptionedImage {

    private String name;
    private Path imagePath;

    private Path captionPath;
    private String caption = "";

    private int numTokens;
    private boolean captionEdited;

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
}
