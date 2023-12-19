package de.mrnotsoevil.sdcaptionstudio.api;

import javax.swing.*;

public class SDCaptionedImageInfo {
    private ImageIcon thumbnail;
    private String size;

    public ImageIcon getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ImageIcon thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
