package de.mrnotsoevil.sdcaptionstudio;

import org.hkijena.jipipe.utils.JIPipeResourceManager;

import javax.swing.*;
import java.awt.*;

public class SDCaptionStudio {
    public static JIPipeResourceManager RESOURCES = new JIPipeResourceManager(SDCaptionStudio.class, "/de/mrnotsoevil/sdcaptionstudio");
    private static SDCaptionStudio INSTANCE;
    private final ImageIcon applicationIcon;

    private SDCaptionStudio() {
        this.applicationIcon = new ImageIcon(RESOURCES.getResourceURL("icon-128.png"));
    }

    public static SDCaptionStudio getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new SDCaptionStudio();
        }
        return INSTANCE;
    }

    public ImageIcon getApplicationIcon() {
        return applicationIcon;
    }
}
