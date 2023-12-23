package de.mrnotsoevil.sdcaptionstudio.ui.utils;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SDCaptionUtils {

    public static final char[] TEMPLATE_SEPARATOR_CHARS = {',', ';', '.', '@', ' ', '\n', '\t', '\r'};
    public static JIPipeResourceManager RESOURCES = new JIPipeResourceManager(SDCaptionUtils.class, "/de/mrnotsoevil/sdcaptionstudio");
    private static SDCaptionUtils INSTANCE;
    private static ImageIcon APPLICATION_ICON;

    private static Theme RSYNTAX_THEME_DEFAULT;
    private static Theme RSYNTAX_THEME_DARK;

    private SDCaptionUtils() {

    }

    public static SDCaptionUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SDCaptionUtils();
        }
        return INSTANCE;
    }

    public static ImageIcon getApplicationIcon() {
        if (APPLICATION_ICON == null) {
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

    public static boolean isValidTemplateContent(String content) {
        if(content == null) {
            return false;
        }
        content = content.trim();
        return !StringUtils.isNullOrEmpty(content);
    }

    public static String toValidTemplateKey(String name) {
        name = name.replaceAll("\\s+", "_");
        for (char c : TEMPLATE_SEPARATOR_CHARS) {
            name = name.replace(c, '_');
        }
        return name.trim();
    }

    public static Set<String> walkInternalResourceFolder(String folder) {
        String globalFolder = RESOURCES.getBasePath() + "/" + folder;
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("de.mrnotsoevil.sdcaptionstudio"))
                .setScanners(new ResourcesScanner()));

        Set<String> allResources = reflections.getResources(Pattern.compile(".*"));
        allResources = allResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        return allResources.stream().filter(s -> s.startsWith(globalFolder)).collect(Collectors.toSet());
    }

    public static MarkdownDocument getDocumentation(String name) {
        return MarkdownDocument.fromResourceURL(RESOURCES.getResourceURL("documentation/" + name),
                false,
                Collections.emptyMap());
    }
}
