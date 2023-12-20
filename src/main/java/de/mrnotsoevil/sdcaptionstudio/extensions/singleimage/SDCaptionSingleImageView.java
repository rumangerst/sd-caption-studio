package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.ui.imagelist.SDCaptionedImageListPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;

public class SDCaptionSingleImageView extends SDCaptionProjectWorkbenchPanel implements Disposable {
    private final JPanel propertiesPanel = new JPanel(new BorderLayout());
    private final SDCaptionedImageListPanel imageListPanel;
    private final SDCaptionSingleImageCaptionEditor captionEditorUI;

    public SDCaptionSingleImageView(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.imageListPanel = new SDCaptionedImageListPanel(workbench);
        captionEditorUI = new SDCaptionSingleImageCaptionEditor(this, workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                imageListPanel,
                propertiesPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(400, true));
        add(splitPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        propertiesPanel.setLayout(new BorderLayout());
        propertiesPanel.add(captionEditorUI, BorderLayout.CENTER);

        imageListPanel.addListSelectionListener(this::editCaption);
    }


    public void editCaption(SDCaptionedImage value) {
        if(value != null) {
            captionEditorUI.editCaption(value);
        }
    }

    public void goToPreviousImage() {
        imageListPanel.goToPreviousImage();
    }

    public void goToNextImage() {
        imageListPanel.goToNextImage();
    }
}
