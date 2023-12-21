package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import ij.IJ;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SDCaptionedImage {

    private SDCaptionProject project;
    private String name;
    private Path imagePath;
    private Path captionPath;
    private SDCaptionedImageInfo currentImageInfo;
    private SDCaptionedImageInfoLoader imageInfoLoader;
    private String userCaption;
    private boolean userCaptionEdited = false;

    public SDCaptionedImageInfo getCurrentImageInfo() {
        return currentImageInfo;
    }

    public void setCurrentImageInfo(SDCaptionedImageInfo currentImageInfo) {
        this.currentImageInfo = currentImageInfo;
        if (project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.ImageInfo));
        }
    }

    public SDCaptionedImageInfo getImageInfoForUI() {
        if (currentImageInfo != null) {
            return currentImageInfo;
        } else {
            synchronized (this) {
                if (imageInfoLoader == null) {
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

    @JsonGetter("user-caption")
    public String getUserCaption() {
        return userCaption;
    }

    @JsonSetter("user-caption")
    public void setUserCaption(String userCaption) {
        this.userCaption = userCaption;
        if(project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.Caption));
        }
    }

    @JsonGetter("user-caption-edited")
    public boolean isUserCaptionEdited() {
        return userCaptionEdited;
    }

    @JsonSetter("user-caption-edited")
    public void setUserCaptionEdited(boolean userCaptionEdited) {
        this.userCaptionEdited = userCaptionEdited;
        if(project != null) {
            project.getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, SDCaptionedImageProperty.CaptionEdited));
        }
    }

    public String getCompiledUserCaption() {
        return getProject().compileCaption(StringUtils.nullToEmpty(userCaption));
    }

    public void saveCaptionToFile() throws IOException {
        Files.write(getCaptionPath(), getCompiledUserCaption().getBytes(StandardCharsets.UTF_8));
        setUserCaptionEdited(false);
    }

    public void loadCaptionFromFile(boolean overwrite) {
        if(overwrite || userCaption == null) {
            try {
                if (Files.isRegularFile(captionPath)) {
                    setUserCaption(new String(Files.readAllBytes(captionPath), StandardCharsets.UTF_8));
                    setUserCaptionEdited(false);
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    public int getNumTokens() {
        String compiled = getCompiledUserCaption().trim();
        if(compiled.isEmpty()) {
            return 0;
        }
        else {
            return compiled.split("\\s+").length;
        }
    }

    public String getTokenInfoString() {
        int numTokens = getNumTokens();
        return numTokens + " / " + (int) Math.max(1, Math.ceil(numTokens * 1.0 / 75)) * 75;
    }
}
