package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SDCaptionedImage {

    private SDCaptionProject project;
    private String name;
    private Path imagePath;
    private Path captionPath;
    private SDCaptionedImageInfo currentImageInfo;
    private SDCaptionedImageInfoLoader imageInfoLoader;
    private String userCaption;
    private String savedUserCaption;
    private List<String> savedUserCaptionHistory = new ArrayList<>();

    public SDCaptionedImage() {
    }

    public SDCaptionedImage(SDCaptionedImage other) {
        this.project = other.project;
        this.name = other.name;
        this.imagePath = other.imagePath;
        this.captionPath = other.captionPath;
        this.userCaption = other.userCaption;
        this.savedUserCaption = other.savedUserCaption;
        this.savedUserCaptionHistory = new ArrayList<>(other.savedUserCaptionHistory);
    }

    public SDCaptionedImageInfo getCurrentImageInfo() {
        return currentImageInfo;
    }

    public void setCurrentImageInfo(SDCaptionedImageInfo currentImageInfo) {
        this.currentImageInfo = currentImageInfo;
        emitPropertyChanged(SDCaptionedImageProperty.ImageInfo);
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

    public String getNonNullTrimmedUserCaption() {
        return StringUtils.nullToEmpty(userCaption).trim();
    }

    @JsonSetter("user-caption")
    public void setUserCaption(String userCaption) {
        this.userCaption = userCaption;
        emitPropertyChanged(SDCaptionedImageProperty.UserCaption);
    }

    @JsonGetter("saved-user-caption")
    public String getSavedUserCaption() {
        return savedUserCaption;
    }

    @JsonSetter("saved-user-caption")
    public void setSavedUserCaption(String savedUserCaption) {
        this.savedUserCaption = savedUserCaption;
        emitPropertyChanged(SDCaptionedImageProperty.SavedCaption);
    }

    @JsonGetter("saved-user-caption-history")
    public List<String> getSavedUserCaptionHistory() {
        return savedUserCaptionHistory;
    }

    @JsonSetter("saved-user-caption-history")
    public void setSavedUserCaptionHistory(List<String> savedUserCaptionHistory) {
        this.savedUserCaptionHistory = savedUserCaptionHistory;
    }

    public String getCompiledUserCaption() {
        return getProject().compileCaption(StringUtils.nullToEmpty(userCaption));
    }

    public void saveCaptionToFile() throws IOException {
        String compiledUserCaption = getCompiledUserCaption();
        Files.write(getCaptionPath(), compiledUserCaption.getBytes(StandardCharsets.UTF_8));
        setSavedUserCaption(compiledUserCaption);

        // Add to history
        String s = getNonNullTrimmedUserCaption();
        savedUserCaptionHistory.remove(s);
        savedUserCaptionHistory.add(s);
    }

    public void loadCaptionFromFile(boolean overwrite) {
        if(overwrite || userCaption == null) {
            try {
                if (Files.isRegularFile(captionPath)) {
                    setUserCaption(new String(Files.readAllBytes(captionPath), StandardCharsets.UTF_8));
                    setSavedUserCaption(getUserCaption());
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        else if(savedUserCaption == null) {
            // Restore saved caption
            try {
                if (Files.isRegularFile(captionPath)) {
                    setSavedUserCaption(new String(Files.readAllBytes(captionPath), StandardCharsets.UTF_8));
                    savedUserCaptionHistory.add(getSavedUserCaption());
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

    public boolean isUserCaptionEdited() {
        return !Objects.equals(StringUtils.nullToEmpty(savedUserCaption).trim(), getCompiledUserCaption().trim());
    }

    public void emitPropertyChanged(SDCaptionedImageProperty property) {
        if(getProject() != null) {
            getProject().getCaptionedImagePropertyUpdatedEventEmitter().emit(
                    new SDCaptionedImagePropertyUpdatedEvent(this, property));
        }
    }

    public ImagePlus readAsImageJImage() {
        return IJ.openImage(getImagePath().toString());
    }
}
