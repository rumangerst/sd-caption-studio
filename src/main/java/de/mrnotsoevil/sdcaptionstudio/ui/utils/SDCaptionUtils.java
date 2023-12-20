package de.mrnotsoevil.sdcaptionstudio.ui.utils;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;

public class SDCaptionUtils {
    public static JIPipeResourceManager RESOURCES = new JIPipeResourceManager(SDCaptionUtils.class, "/de/mrnotsoevil/sdcaptionstudio");
    private static SDCaptionUtils INSTANCE;
    private static ImageIcon APPLICATION_ICON;

    private static Theme RSYNTAX_THEME_DEFAULT;
    private static Theme RSYNTAX_THEME_DARK;

    private SDCaptionUtils() {

    }

    public static SDCaptionUtils getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new SDCaptionUtils();
        }
        return INSTANCE;
    }

    public static ImageIcon getApplicationIcon() {
        if(APPLICATION_ICON == null) {
            APPLICATION_ICON = new ImageIcon(RESOURCES.getResourceURL("icon-128.png"));
        }
        return APPLICATION_ICON;
    }

    public static void applyThemeToCodeEditor(RSyntaxTextArea textArea) {
        if (UIUtils.DARK_THEME) {
            try {
                if (RSYNTAX_THEME_DARK == null) {
                    RSYNTAX_THEME_DARK = Theme.load(ResourceUtils.class.getResourceAsStream(
                            RESOURCES.getBasePath() + "/themes/dark.xml"));
                }
                RSYNTAX_THEME_DARK.apply(textArea);
            } catch (IOException ioe) { // Never happens
                ioe.printStackTrace();
            }
        } else {
            try {
                if (RSYNTAX_THEME_DEFAULT == null) {
                    RSYNTAX_THEME_DEFAULT = Theme.load(ResourceUtils.class.getResourceAsStream(
                            RESOURCES.getBasePath() + "/themes/default.xml"));
                }
                RSYNTAX_THEME_DEFAULT.apply(textArea);
            } catch (IOException ioe) { // Never happens
                ioe.printStackTrace();
            }
        }
    }
}