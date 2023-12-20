package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
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
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class SDCaptionSingleImageCaptionEditor extends SDCaptionProjectWorkbenchPanel {
    private final SDCaptionSingleImageView view;
    private SDCaptionedImage currentlyEditedImage;
    private final JIPipeImageViewer imageViewer;
    private final RSyntaxTextArea captionEditor;

    public SDCaptionSingleImageCaptionEditor(SDCaptionSingleImageView view, SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.view = view;
        this.imageViewer = new JIPipeImageViewer(workbench,
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class),
                Collections.emptyMap());


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
        this.captionEditor = new RSyntaxTextArea(new RSyntaxDocument(tokenMakerFactory, "text/caption"));

        // Remove the 2d/3d switcher
        imageViewer.getToolBar().remove(0);

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel promptEditorPanel = new JPanel(new BorderLayout());
        add(new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, imageViewer, promptEditorPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(350, false)), BorderLayout.CENTER);


        SDCaptionUtils.applyThemeToCodeEditor(captionEditor);
        captionEditor.setLineWrap(true);
        captionEditor.setWrapStyleWord(false);
        captionEditor.setTabSize(4);
        captionEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        captionEditor.setBackground(UIManager.getColor("TextArea.background"));
        captionEditor.setHighlightCurrentLine(true);
        promptEditorPanel.add(new RTextScrollPane(captionEditor), BorderLayout.CENTER);

        JToolBar topToolbar = new JToolBar();
        topToolbar.setFloatable(false);
        promptEditorPanel.add(topToolbar,BorderLayout.NORTH);

        JToolBar bottomToolbar = new JToolBar();
        bottomToolbar.setFloatable(false);
        promptEditorPanel.add(bottomToolbar, BorderLayout.SOUTH);
        initializeBottomToolbar(bottomToolbar);
    }

    private void initializeBottomToolbar(JToolBar bottomToolbar) {
        JButton previousButton = UIUtils.createButton("Previous", UIUtils.getIconFromResources("actions/previous.png"), view::goToPreviousImage);
        bottomToolbar.add(previousButton);

        bottomToolbar.add(Box.createHorizontalGlue());

        JButton nextButton = UIUtils.createButton("Next", UIUtils.getIconFromResources("actions/next.png"), view::goToNextImage);
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        bottomToolbar.add(nextButton);
    }

    public void editCaption(SDCaptionedImage captionedImage) {
        this.currentlyEditedImage = captionedImage;
        this.captionEditor.setText(captionedImage.getCaption());
        reload();
    }

    public void reload() {
        try(BusyCursor cursor = new BusyCursor(SwingUtilities.getWindowAncestor(this))) {
            ImagePlus image = IJ.openImage(currentlyEditedImage.getImagePath().toString());
            imageViewer.setImagePlus(image);
        }
        catch (Exception e) {
            IJ.handleException(e);
        }
    }
}
