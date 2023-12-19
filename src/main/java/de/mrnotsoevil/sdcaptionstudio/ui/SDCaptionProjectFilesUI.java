package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.AdvancedFileChooser;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SDCaptionProjectFilesUI extends JIPipeWorkbenchPanel {
    private final SDCaptionProjectUI projectUI;
    private final JList<SDCaptionedImage> imageList = new JList<>();

    public SDCaptionProjectFilesUI(SDCaptionProjectUI projectUI) {
        super(projectUI.getWorkbench());
        this.projectUI = projectUI;
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JScrollPane(imageList), BorderLayout.CENTER);
        imageList.setCellRenderer(new SDCaptionedImageListCellRenderer(projectUI.getProject()));
    }

    public void reload() {
        DefaultListModel<SDCaptionedImage> model = new DefaultListModel<>();
        List<SDCaptionedImage> images = new ArrayList<>(projectUI.getProject().getImages().values());
        images.sort(Comparator.comparing(SDCaptionedImage::getName, NaturalOrderComparator.INSTANCE));
        for (SDCaptionedImage image : images) {
            model.addElement(image);
        }
        imageList.setModel(model);
    }

    public SDCaptionProjectUI getProjectUI() {
        return projectUI;
    }
}
