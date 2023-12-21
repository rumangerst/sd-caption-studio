package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionTemplate;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionFlexContentPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.templatemanager.SDCaptionTemplateManagerPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionSyntaxTokenMaker;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import ij.IJ;
import ij.ImagePlus;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FlexContentPanel;
import org.hkijena.jipipe.ui.components.icons.NewThrobberIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SDCaptionSingleImageCaptionEditor extends SDCaptionProjectWorkbenchPanel implements SDCaptionedImagePropertyUpdatedEventListener, SDCaptionProjectTemplatesChangedEventListener {
    private final SDCaptionSingleImageView view;
    private final JIPipeImageViewer imageViewer;
    private final RSyntaxTextArea captionEditor;
    private final RSyntaxTextArea captionPreview;
    private final JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
    private final Icon autoSaveDefaultIcon = UIUtils.getIconFromResources("actions/view-refresh.png");
    private final NewThrobberIcon autoSaveSavingIcon = new NewThrobberIcon(this);
    private final JToggleButton autoSaveToggle = new JToggleButton("Auto-save", UIUtils.getIconFromResources("actions/view-refresh.png"), true);
    private final Timer captionSaveTimer;
    private SDCaptionedImage currentlyEditedImage;
    private boolean loading;
    private final SDCaptionTemplateManagerPanel templateManagerPanel;

    public SDCaptionSingleImageCaptionEditor(SDCaptionSingleImageView view, SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.view = view;
        this.imageViewer = new JIPipeImageViewer(workbench,
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class),
                Collections.emptyMap());
        this.captionSaveTimer = new Timer(750, e -> {
            saveCaptionImmediately();
        });
        this.captionSaveTimer.setRepeats(false);


        SDCaptionSyntaxTokenMaker tokenMaker = new SDCaptionSyntaxTokenMaker();
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/caption");
            }
        };
        this.captionEditor = createCaptionEditor();
        this.captionPreview = createCaptionEditor();
        this.templateManagerPanel = new SDCaptionTemplateManagerPanel(getProjectWorkbench());

        // Remove the 2d/3d switcher
        imageViewer.getToolBar().remove(0);

        getProject().getCaptionedImagePropertyUpdatedEventEmitter().subscribe(this);
        getProject().getProjectTemplatesChangedEventEmitter().subscribe(this);

        initialize();

    }

    private static RSyntaxTextArea createCaptionEditor() {
        SDCaptionSyntaxTokenMaker tokenMaker = new SDCaptionSyntaxTokenMaker();
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/caption");
            }
        };
        return new RSyntaxTextArea(new RSyntaxDocument(tokenMakerFactory, "text/caption"));
    }

    private void saveCaptionImmediately() {
        autoSaveSavingIcon.stop();
        autoSaveToggle.setIcon(autoSaveDefaultIcon);
        if (currentlyEditedImage != null) {
            try {
                currentlyEditedImage.saveCaptionToFile();
                getProjectWorkbench().sendStatusBarText("Saved " + currentlyEditedImage.getCaptionPath());
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel promptEditorPanel = new JPanel(new BorderLayout(8, 8));
        add(new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, imageViewer, promptEditorPanel,
                new AutoResizeSplitPane.FixedRatio(0.66)), BorderLayout.CENTER);

        SDCaptionFlexContentPanel promptEditorContentPanel = new SDCaptionFlexContentPanel(FlexContentPanel.WITH_SIDEBAR);
        promptEditorContentPanel.setSidebarRatio(new AutoResizeSplitPane.FixedRatio(0.5));
        promptEditorPanel.add(promptEditorContentPanel, BorderLayout.CENTER);
        promptEditorPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(24, 8, 8, 8),
                BorderFactory.createEtchedBorder()));

        initializeCaptionEditor(promptEditorContentPanel);
        initializeCaptionPreview(promptEditorContentPanel.getSideBar());
        initializeTemplateManager(promptEditorContentPanel.getSideBar());

        initializeTopToolbar(promptEditorContentPanel.getToolBar());

        JToolBar bottomToolbar = new JToolBar();
        bottomToolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottomToolbar.setFloatable(false);
        promptEditorPanel.add(bottomToolbar, BorderLayout.SOUTH);
        initializeBottomToolbar(bottomToolbar);
    }

    private void initializeTemplateManager(DocumentTabPane sideBar) {


//        managerPanel.getToolBar().add(Box.createHorizontalStrut(8), 0);

        JButton templateFromSelectionButton = UIUtils.createButton("From selection", UIUtils.getIconFromResources("actions/go-next-context.png"),
                this::newTemplateFromSelection);
        templateManagerPanel.getToolBar().add(templateFromSelectionButton, 0);

        JButton insertTemplateButton = UIUtils.createButton("Insert", UIUtils.getIconFromResources("actions/go-previous-context.png"),
                this::insertTemplate);
        templateManagerPanel.getToolBar().add(insertTemplateButton, 0);

        templateManagerPanel.getTemplateJList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    insertTemplate();
                }
            }
        });

        sideBar.addTab("Templates",
                UIUtils.getIconFromResources("actions/games-highscores.png"),
                templateManagerPanel,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    public void insertCaptionTextAtCaret(String text, boolean spacing) {
        int caret = this.captionEditor.getCaretPosition();
        if (caret > 0) {
            try {
                if (spacing && !Objects.equals(this.captionEditor.getText(caret - 1, 1), " ")) {
                    text = ", " + text;
                }

            } catch (BadLocationException e) {
            }
        }

        try {
            if (spacing && !Objects.equals(this.captionEditor.getText(caret, 1), " ")) {
                text = text + " ";
            }
        } catch (BadLocationException e) {
        }

        this.captionEditor.insert(text, this.captionEditor.getCaretPosition());
        this.captionEditor.requestFocusInWindow();
    }

    private void insertTemplate() {
        if(currentlyEditedImage != null) {
            SDCaptionTemplate template = templateManagerPanel.getTemplateJList().getSelectedValue();
            if(template != null) {
                if(!currentlyEditedImage.getUserCaption().contains("@" + template.getKey())) {
                    insertCaptionTextAtCaret("@" + template.getKey(), true);
                    getProjectWorkbench().sendStatusBarText("Inserted template @" + template.getKey() + " into " + currentlyEditedImage.getName());
                }
                else {
                    getProjectWorkbench().sendStatusBarText("The template @" + template.getKey() + " already exists in " + currentlyEditedImage.getName());
                }
            }
        }
    }

    private void newTemplateFromSelection() {
        if(currentlyEditedImage != null) {
            String selectedText = captionEditor.getSelectedText();
            templateManagerPanel.createNewTemplate(selectedText);
        }
    }

    private void initializeCaptionPreview(DocumentTabPane sideBar) {
        SDCaptionUtils.applyThemeToCodeEditor(captionPreview);
        captionPreview.setLineWrap(true);
        captionPreview.setWrapStyleWord(false);
        captionPreview.setTabSize(4);
        captionPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        captionPreview.setBackground(UIManager.getColor("TextArea.background"));
        captionPreview.setEditable(false);
        sideBar.addTab("Preview", UIUtils.getIconFromResources("actions/search.png"),
                new RTextScrollPane(captionPreview), DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeCaptionEditor(SDCaptionFlexContentPanel promptEditorContentPanel) {
        SDCaptionUtils.applyThemeToCodeEditor(captionEditor);
        captionEditor.setLineWrap(true);
        captionEditor.setWrapStyleWord(false);
        captionEditor.setTabSize(4);
        captionEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        captionEditor.setBackground(UIManager.getColor("TextArea.background"));
        captionEditor.setHighlightCurrentLine(true);
        captionEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!loading) {
                    updateCaptionFromTextEditor();
                }
            }
        });
        promptEditorContentPanel.getContentPanel().add(new RTextScrollPane(captionEditor), BorderLayout.CENTER);
    }

    private void initializeTopToolbar(JToolBar topToolbar) {

        topToolbar.add(Box.createHorizontalGlue());

        saveButton.addActionListener(e -> saveCaptionImmediately());
        topToolbar.add(saveButton);

        autoSaveToggle.addActionListener(e -> {
            if (autoSaveToggle.isSelected()) {
                saveCaptionLater();
            }
        });
        topToolbar.add(autoSaveToggle);
    }

    private void saveCaptionLater() {
        autoSaveToggle.setIcon(autoSaveSavingIcon);
        autoSaveSavingIcon.start();
        captionSaveTimer.restart();
    }

    private void updateCaptionFromTextEditor() {
        if (currentlyEditedImage != null) {
            currentlyEditedImage.setUserCaption(captionEditor.getText());
            currentlyEditedImage.setUserCaptionEdited(true);

            // Update preview
            captionPreview.setText(currentlyEditedImage.getCompiledUserCaption());
        }
    }

    private void initializeBottomToolbar(JToolBar bottomToolbar) {
        JButton previousButton = UIUtils.createButton("Previous", UIUtils.getIconFromResources("actions/previous.png"), view::goToPreviousImage);
        previousButton.setToolTipText("<html>Goes to the previous image</html>");
        bottomToolbar.add(previousButton);

        bottomToolbar.add(Box.createHorizontalGlue());

        JButton nextButton = UIUtils.createButton("Next", UIUtils.getIconFromResources("actions/next.png"), view::goToNextImage);
        previousButton.setToolTipText("<html>Goes to the next image</html>");
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        bottomToolbar.add(nextButton);
    }

    public void editCaption(SDCaptionedImage captionedImage) {
        if (currentlyEditedImage != null && captionSaveTimer.isRunning()) {
            captionSaveTimer.stop();
            autoSaveToggle.setIcon(autoSaveDefaultIcon);
            getProjectWorkbench().sendStatusBarText("Auto-save of " + currentlyEditedImage.getName() + " was interrupted!");
        }
        this.currentlyEditedImage = captionedImage;
        reload();
    }

    public void reload() {
        loading = true;
        try (BusyCursor cursor = new BusyCursor(SwingUtilities.getWindowAncestor(this))) {
            if (currentlyEditedImage != null) {
                this.captionEditor.setText(currentlyEditedImage.getUserCaption());
                this.captionPreview.setText(currentlyEditedImage.getCompiledUserCaption());
                ImagePlus image = IJ.openImage(currentlyEditedImage.getImagePath().toString());
                imageViewer.setImagePlus(image);
            }
        } catch (Exception e) {
            IJ.handleException(e);
        } finally {
            loading = false;
        }
    }

    @Override
    public void onCaptionedImageInfoUpdated(SDCaptionedImagePropertyUpdatedEvent event) {
        if (event.getImage() == currentlyEditedImage && currentlyEditedImage.isUserCaptionEdited() && autoSaveToggle.isSelected()) {
            saveCaptionLater();
        }
    }

    @Override
    public void onProjectTemplatesChanged(SDCaptionProjectTemplatesChangedEvent event) {
        reload();
    }
}
