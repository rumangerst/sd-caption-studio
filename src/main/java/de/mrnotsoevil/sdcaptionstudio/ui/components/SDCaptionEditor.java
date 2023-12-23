package de.mrnotsoevil.sdcaptionstudio.ui.components;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionSyntaxTokenMaker;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

public class SDCaptionEditor extends RSyntaxTextArea {

    private final SDCaptionProjectWorkbench projectWorkbench;
    public SDCaptionEditor(SDCaptionProjectWorkbench projectWorkbench) {
        super(createDocument());
        this.projectWorkbench = projectWorkbench;

        SDCaptionUtils.applyThemeToCodeEditor(this);
        this.setLineWrap(true);
        this.setWrapStyleWord(false);
        this.setTabSize(4);
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        this.setBackground(UIManager.getColor("TextArea.background"));
    }

    private static RSyntaxDocument createDocument() {
        SDCaptionSyntaxTokenMaker tokenMaker = new SDCaptionSyntaxTokenMaker();
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/sd-caption");
            }
        };
        return new RSyntaxDocument(tokenMakerFactory, "text/sd-caption");
    }

    public SDCaptionProjectWorkbench getProjectWorkbench() {
        return projectWorkbench;
    }

    public SDCaptionProject getProject() {
        return projectWorkbench.getProject();
    }
}
