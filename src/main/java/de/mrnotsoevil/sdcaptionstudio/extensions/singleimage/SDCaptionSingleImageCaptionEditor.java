package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionFlexContentPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
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
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class SDCaptionSingleImageCaptionEditor extends SDCaptionProjectWorkbenchPanel implements SDCaptionedImagePropertyUpdatedEventListener {
    private final SDCaptionSingleImageView view;
    private SDCaptionedImage currentlyEditedImage;
    private final JIPipeImageViewer imageViewer;
    private final RSyntaxTextArea captionEditor;
    private final RSyntaxTextArea captionPreview;
    private final JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));

    private final Icon autoSaveDefaultIcon = UIUtils.getIconFromResources("actions/view-refresh.png");
    private final NewThrobberIcon autoSaveSavingIcon = new NewThrobberIcon(this);
    private final JToggleButton autoSaveToggle = new JToggleButton("Auto-save", UIUtils.getIconFromResources("actions/view-refresh.png"), true);
    private boolean loading;

    private final Timer captionSaveTimer;

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

        // Remove the 2d/3d switcher
        imageViewer.getToolBar().remove(0);

        getProject().getCaptionedImagePropertyUpdatedEventEmitter().subscribe(this);

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
                currentlyEditedImage.saveCaption();
                getProjectWorkbench().sendStatusBarText("Saved " + currentlyEditedImage.getCaptionPath());
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel promptEditorPanel = new JPanel(new BorderLayout(8,8));
        add(new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, imageViewer, promptEditorPanel,
                new AutoResizeSplitPane.FixedRatio(0.66)), BorderLayout.CENTER);

        SDCaptionFlexContentPanel promptEditorContentPanel = new SDCaptionFlexContentPanel(FlexContentPanel.WITH_SIDEBAR);
        promptEditorContentPanel.setSidebarRatio(new AutoResizeSplitPane.FixedRatio(0.5));
        promptEditorPanel.add(promptEditorContentPanel, BorderLayout.CENTER);
        promptEditorPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(24,8,8,8),
                BorderFactory.createEtchedBorder()));

        initializeCaptionEditor(promptEditorContentPanel);
        initializeCaptionPreview(promptEditorContentPanel.getSideBar());

        initializeTopToolbar(promptEditorContentPanel.getToolBar());

        JToolBar bottomToolbar = new JToolBar();
        bottomToolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottomToolbar.setFloatable(false);
        promptEditorPanel.add(bottomToolbar, BorderLayout.SOUTH);
        initializeBottomToolbar(bottomToolbar);
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
            if(autoSaveToggle.isSelected()) {
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
            currentlyEditedImage.setEditedCaption(captionEditor.getText());

            // Update preview
            captionPreview.setText(getProject().captionExpandTemplates(captionEditor.getText()));
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
        if(currentlyEditedImage != null && captionSaveTimer.isRunning()) {
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
                this.captionEditor.setText(currentlyEditedImage.getShortenedCaption());
                this.captionPreview.setText(currentlyEditedImage.getFinalCaption());
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
        if(event.getImage() == currentlyEditedImage && currentlyEditedImage.isCaptionEdited() && autoSaveToggle.isSelected()) {
            saveCaptionLater();
        }
    }
}
