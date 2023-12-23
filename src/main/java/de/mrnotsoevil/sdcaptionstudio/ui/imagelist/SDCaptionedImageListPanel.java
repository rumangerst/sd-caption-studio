package de.mrnotsoevil.sdcaptionstudio.ui.imagelist;

import com.google.common.primitives.Ints;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectReloadedEventListener;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionedImagePropertyUpdatedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SDCaptionedImageListPanel extends SDCaptionProjectWorkbenchPanel implements SDCaptionedImagePropertyUpdatedEventListener, SDCaptionProjectReloadedEventListener {
    private final JList<SDCaptionedImage> imageList = new JList<>();
    private final SearchTextField searchTextField = new SearchTextField();
    private final boolean compact;

    public SDCaptionedImageListPanel(SDCaptionProjectWorkbench workbench, boolean compact) {
        super(workbench);
        this.compact = compact;
        initialize();
        workbench.getProject().getCaptionedImagePropertyUpdatedEventEmitter().subscribe(this);
        workbench.getProject().getProjectReloadedEventEmitter().subscribe(this);
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JScrollPane(imageList), BorderLayout.CENTER);
        imageList.setCellRenderer(compact ? new SDCaptionedCompactImageListCellRenderer(getProject())
                : new SDCaptionedFullImageListCellRenderer(getProject()));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(searchTextField);
        searchTextField.addActionListener(e -> reload());

        imageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    int i = imageList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        if(!Ints.contains(imageList.getSelectedIndices(), i)) {
                            imageList.clearSelection();
                            imageList.addSelectionInterval(i, i);
                        }
                    }

                    JPopupMenu menu = new JPopupMenu();
                    createContextMenu(menu);
                    menu.show(imageList, e.getX(), e.getY());
                }
            }
        });
    }

    private void createContextMenu(JPopupMenu menu) {
        List<SDCaptionedImage> images = imageList.getSelectedValuesList();
        menu.add(UIUtils.createMenuItem("Select all", "Selects all items", UIUtils.getIconFromResources("actions/stock_select-all.png"), this::selectAll));
        menu.add(UIUtils.createMenuItem("Clear selection", "Selects no items", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
        menu.add(UIUtils.createMenuItem("Invert selection", "Inverts the selection", UIUtils.getIconFromResources("actions/edit-select-invert.png"), this::invertSelection));
        if(!images.isEmpty()) {
            menu.addSeparator();
            menu.add(UIUtils.createMenuItem("Duplicate", "Duplicates the selected images", UIUtils.getIconFromResources("actions/edit-duplicate.png"), this::duplicateSelection));
            menu.add(UIUtils.createMenuItem("Rename", "Renames the selected images", UIUtils.getIconFromResources("actions/edit-rename.png"), this::renameSelection));
            menu.add(UIUtils.createMenuItem("Delete", "Deletes the selected images", UIUtils.getIconFromResources("actions/delete.png"), this::deleteSelection));

        }

    }

    public void renameSelection() {
        List<SDCaptionedImage> images = imageList.getSelectedValuesList();
        if(images.size() == 1) {
            String newName = JOptionPane.showInputDialog(this, "Set the name of the new image:",
                    images.get(0).getName());
            if(getProject().getImages().containsKey(newName)) {
                JOptionPane.showMessageDialog(this,
                        "There is already an image with the name '" + newName + "'",
                        "Duplicate",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            getProject().renameImage(images.get(0).getName(), newName);
        }
        else {
            SDCaptionedImageRenamingUtil util = SDCaptionedImageRenamingUtil.INSTANCE;
            if (ParameterPanel.showDialog(getProjectWorkbench(), util, SDCaptionUtils.getDocumentation("bulk-rename.md"),
                    "Rename multiple images", ParameterPanel.DEFAULT_DIALOG_FLAGS)) {
                try {
                    util.apply(getProject(), images);
                }
                catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Rename", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void deleteSelection() {
        List<SDCaptionedImage> images = imageList.getSelectedValuesList();
        if(!images.isEmpty()) {
            if(JOptionPane.showConfirmDialog(this, "Do you really want to permanently delete the " +
                    "selected image(s)?\nTip: Use the backup function in the top-right corner to quickly create a copy of all your files.",
                    "Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (SDCaptionedImage image : images) {
                    getProject().deleteImage(image.getName());
                }
            }
        }
    }

    public void duplicateSelection() {
        List<SDCaptionedImage> images = imageList.getSelectedValuesList();
        if(images.size() == 1) {
            String newName = JOptionPane.showInputDialog(this, "Set the name of the new image:",
                    StringUtils.makeUniqueString(images.get(0).getName(), "_", getProject().getImages().keySet()));
            if(getProject().getImages().containsKey(newName)) {
                JOptionPane.showMessageDialog(this,
                        "There is already an image with the name '" + newName + "'",
                        "Duplicate",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            getProject().duplicateImage(images.get(0).getName(), newName);
        }
        else if(!images.isEmpty()) {
            for (SDCaptionedImage image : images) {
                getProject().duplicateImage(image.getName(), null);
            }
        }
    }

    public void invertSelection() {
        JList<SDCaptionedImage> imageList = getImageList();
        if(imageList.getModel().getSize() > 0) {
            Set<Integer> selectedIndices = Arrays.stream(imageList.getSelectedIndices()).boxed().collect(Collectors.toSet());
            imageList.clearSelection();
            Set<Integer> newSelectedIndices = new HashSet<>();
            for (int i = 0; i < imageList.getModel().getSize(); i++) {
                if (!selectedIndices.contains(i))
                    newSelectedIndices.add(i);
            }
            imageList.setSelectedIndices(Ints.toArray(newSelectedIndices));
        }
    }

    public void selectNone() {
        imageList.clearSelection();
    }

    public void selectAll() {
        if(imageList.getModel().getSize() > 0) {
            imageList.setSelectionInterval(0, imageList.getModel().getSize() - 1);
        }
    }

    public void addListSelectionListener(Consumer<SDCaptionedImage> consumer) {
        imageList.addListSelectionListener(e -> consumer.accept(imageList.getSelectedValue()));
    }

    public void reload() {
        DefaultListModel<SDCaptionedImage> model = new DefaultListModel<>();
        List<SDCaptionedImage> images = getProject().getSortedImages();
        for (SDCaptionedImage image : images) {
            if (searchTextField.test(image.getName())) {
                model.addElement(image);
            }
        }
        imageList.setModel(model);
    }

    @Override
    public void onCaptionedImagePropertyUpdated(SDCaptionedImagePropertyUpdatedEvent event) {
        if (imageList.isDisplayable()) {
            imageList.repaint();
        }
    }

    @Override
    public void onProjectReloaded(SDCaptionProjectReloadedEvent event) {
        reload();
    }

    public void goToPreviousImage() {
        if (imageList.getModel().getSize() > 0) {
            int selectedIndex = imageList.getSelectedIndex();
            if (selectedIndex == -1) {
                imageList.setSelectedIndex(0);
            } else if (selectedIndex == 0) {
                imageList.setSelectedIndex(imageList.getModel().getSize() - 1);
            } else {
                imageList.setSelectedIndex(selectedIndex - 1);
            }
            imageList.ensureIndexIsVisible(imageList.getSelectedIndex());
        }
    }

    public void goToNextImage() {
        if (imageList.getModel().getSize() > 0) {
            int selectedIndex = imageList.getSelectedIndex();
            if (selectedIndex == -1) {
                imageList.setSelectedIndex(0);
            } else {
                imageList.setSelectedIndex((selectedIndex + 1) % imageList.getModel().getSize());
            }
            imageList.ensureIndexIsVisible(imageList.getSelectedIndex());
        }
    }

    public JList<SDCaptionedImage> getImageList() {
        return imageList;
    }

    public void display(SDCaptionedImage image) {
        JIPipeDataTable dataTable = new JIPipeDataTable(ImagePlusData.class);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        annotations.add(new JIPipeTextAnnotation("Caption", image.getUserCaption()));
        annotations.add(new JIPipeTextAnnotation("Caption (preview)", image.getCompiledUserCaption()));
        annotations.add(new JIPipeTextAnnotation("Caption (currently saved)", image.getSavedUserCaption()));
        ImagePlusData imagePlusData = new ImagePlusData(image.readAsImageJImage());
        dataTable.addData(imagePlusData, annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting,
                new JIPipeMutableDataContext("project"), new JIPipeProgressInfo());
//        imagePlusData.display("Image viewer: " + image.getName(), getProjectWorkbench(), new JIPipeDataTableDataSource(dataTable, 0));
        JIPipeImageViewer.showImage(getProjectWorkbench(), imagePlusData.getImage(), "Image viewer: " + image.getName());
    }
}
