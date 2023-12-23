package de.mrnotsoevil.sdcaptionstudio.ui.components;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionAutocompleteTag;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTagsChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTagsChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionSyntaxTokenMaker;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class SDCaptionEditor extends RSyntaxTextArea implements SDCaptionProjectTagsChangedEventListener, SDCaptionProjectTemplatesChangedEventListener {

    private final SDCaptionProjectWorkbench projectWorkbench;
    private final DefaultCompletionProvider completionProvider = new DefaultCompletionProvider();
    public SDCaptionEditor(SDCaptionProjectWorkbench projectWorkbench) {
        super(createDocument());
        this.projectWorkbench = projectWorkbench;

        SDCaptionUtils.applyThemeToCodeEditor(this);
        this.setLineWrap(true);
        this.setWrapStyleWord(false);
        this.setTabSize(4);
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        this.setBackground(UIManager.getColor("TextArea.background"));

        AutoCompletion ac = new AutoCompletion(completionProvider);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(true);
        ac.install(this);

        updateCompletions();
        getProject().getProjectTagsChangedEventEmitter().subscribe(this);
        getProject().getProjectTemplatesChangedEventEmitter().subscribe(this);
    }

    private void updateCompletions() {
        completionProvider.clear();
        List<Completion> completionList = new ArrayList<>();
        for (String key : getProject().getTemplates().keySet()) {
            completionList.add(new BasicCompletion(completionProvider, "@" + key));
        }

        Set<String> usedTags = new HashSet<>();
        for (SDCaptionAutocompleteTag tag : getProject().getTags()) {
            String key = tag.getKey();
            if (!StringUtils.isNullOrEmpty(key) && !usedTags.contains(key)) {
                usedTags.add(key);
                if(!StringUtils.isNullOrEmpty(tag.getReplacement())) {
                    completionList.add(new ShorthandCompletion(completionProvider, key, tag.getReplacement()));
                }
                else {
                    completionList.add(new BasicCompletion(completionProvider, key));
                }
            }
        }

        completionProvider.addCompletions(completionList);

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

    @Override
    public void onProjectTagsChanged(SDCaptionProjectTagsChangedEvent event) {
        updateCompletions();
    }

    @Override
    public void onProjectTemplatesChanged(SDCaptionProjectTemplatesChangedEvent event) {
        updateCompletions();
    }
}
