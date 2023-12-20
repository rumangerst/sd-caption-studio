package de.mrnotsoevil.sdcaptionstudio.ui.imagelist;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEventListener;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImageInfoUpdatedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImageInfoUpdatedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SDCaptionedImageListPanel extends SDCaptionProjectWorkbenchPanel implements SDCaptionedImageInfoUpdatedEventListener, SDCaptionProjectReloadedEventListener {
    private final JList<SDCaptionedImage> imageList = new JList<>();
    private final SearchTextField searchTextField = new SearchTextField();

    public SDCaptionedImageListPanel(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        initialize();
        workbench.getProject().getCaptionedImageInfoUpdatedEventEmitter().subscribe(this);
        workbench.getProject().getProjectReloadedEventEmitter().subscribe(this);
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JScrollPane(imageList), BorderLayout.CENTER);
        imageList.setCellRenderer(new SDCaptionedImageListCellRenderer(getProject()));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(searchTextField);
        searchTextField.addActionListener(e -> reload());
    }

    public void addListSelectionListener(Consumer<SDCaptionedImage> consumer) {
        imageList.addListSelectionListener(e -> consumer.accept(imageList.getSelectedValue()));
    }

    public void reload() {
        DefaultListModel<SDCaptionedImage> model = new DefaultListModel<>();
        List<SDCaptionedImage> images = new ArrayList<>(getProject().getImages().values());
        images.sort(Comparator.comparing(SDCaptionedImage::getName, NaturalOrderComparator.INSTANCE));
        for (SDCaptionedImage image : images) {
            if(searchTextField.test(image.getName())) {
                model.addElement(image);
            }
        }
        imageList.setModel(model);
    }

    @Override
    public void onCaptionedImageInfoUpdated(SDCaptionedImageInfoUpdatedEvent event) {
        if(imageList.isDisplayable()) {
            imageList.repaint();
        }
    }

    @Override
    public void onProjectReloaded(SDCaptionProjectReloadedEvent event) {
        reload();
    }

    public void goToPreviousImage() {
        if(imageList.getModel().getSize() > 0) {
            int selectedIndex = imageList.getSelectedIndex();
            if (selectedIndex == -1) {
                imageList.setSelectedIndex(0);
            }
            else if(selectedIndex == 0) {
                imageList.setSelectedIndex(imageList.getModel().getSize() - 1);
            }
            else {
                imageList.setSelectedIndex(selectedIndex - 1);
            }
            imageList.ensureIndexIsVisible(imageList.getSelectedIndex());
        }
    }

    public void goToNextImage() {
        if(imageList.getModel().getSize() > 0) {
            int selectedIndex = imageList.getSelectedIndex();
            if (selectedIndex == -1) {
                imageList.setSelectedIndex(0);
            }
            else {
                imageList.setSelectedIndex((selectedIndex + 1) % imageList.getModel().getSize());
            }
            imageList.ensureIndexIsVisible(imageList.getSelectedIndex());
        }
    }
}
