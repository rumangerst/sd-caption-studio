package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.imagelist.SDCaptionedImagePropertyListPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;

public class SDCaptionSingleImageView extends SDCaptionProjectWorkbenchPanel implements Disposable {
    private final JPanel propertiesPanel = new JPanel(new BorderLayout());
    private final SDCaptionedImagePropertyListPanel imageListPanel;
    private final SDCaptionSingleImageCaptionEditor captionEditorUI;

    public SDCaptionSingleImageView(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.imageListPanel = new SDCaptionedImagePropertyListPanel(workbench);
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

        // Register keyboard shortcuts
//        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//        ActionMap actionMap = getActionMap();
//
//        actionMap.put("next-image", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                goToNextImage();
//            }
//        });
//        actionMap.put("previous-image", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                goToPreviousImage();
//            }
//        });
//
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK), "next-image");
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK), "previous-image");
    }


    public void editCaption(SDCaptionedImage value) {
        if (value != null) {
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
