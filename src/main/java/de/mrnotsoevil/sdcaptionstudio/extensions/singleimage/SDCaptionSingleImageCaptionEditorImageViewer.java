package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.LUTManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

public class SDCaptionSingleImageCaptionEditorImageViewer extends SDCaptionProjectWorkbenchPanel {
    private SDCaptionedImage currentlyEditedImage;
    private final JIPipeImageViewer imageViewer;

    public SDCaptionSingleImageCaptionEditorImageViewer(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.imageViewer = new JIPipeImageViewer(workbench,
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class),
                Collections.emptyMap());

        // Remove the 2d/3d switcher
        imageViewer.getToolBar().remove(0);

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel promptEditorPanel = new JPanel(new BorderLayout());
        add(new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, imageViewer, promptEditorPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(250, false)), BorderLayout.CENTER);
    }

    public void editCaption(SDCaptionedImage captionedImage) {
        this.currentlyEditedImage = captionedImage;
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
