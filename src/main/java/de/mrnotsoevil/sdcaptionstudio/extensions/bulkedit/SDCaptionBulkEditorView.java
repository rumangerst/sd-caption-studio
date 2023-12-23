package de.mrnotsoevil.sdcaptionstudio.extensions.bulkedit;

import com.google.common.primitives.Ints;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.imagelist.SDCaptionedImageListPanel;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SDCaptionBulkEditorView extends SDCaptionProjectWorkbenchPanel {

    private final SDCaptionedImageListPanel listPanel;
    public SDCaptionBulkEditorView(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.listPanel = new SDCaptionedImageListPanel(workbench, false);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Ribbon ribbon = new Ribbon(3);
        initializeRibbon(ribbon);
        add(ribbon, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);

        listPanel.getImageList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    listPanel.display(listPanel.getImageList().getSelectedValue());
                }
            }
        });
    }

    private void initializeRibbon(Ribbon ribbon) {
        Ribbon.Task generalTask = ribbon.addTask("General");

        Ribbon.Band selectionBand = generalTask.addBand("Selection");
        selectionBand.add(new LargeButtonAction("Select all", "Selects all images", UIUtils.getIcon32FromResources("actions/stock_select-all.png"), listPanel::selectAll));
        selectionBand.add(new SmallButtonAction("Clear selection", "Clears the selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), listPanel::selectNone));
        selectionBand.add(new SmallButtonAction("Invert selection", "Inverts the selection", UIUtils.getIconFromResources("actions/edit-select-invert.png"), listPanel::invertSelection));

        Ribbon.Band entriesBand = generalTask.addBand("Entries");
        entriesBand.add(new LargeButtonAction("Rename", "Renames the selected image(s)", UIUtils.getIcon32FromResources("actions/edit-rename.png"), listPanel::renameSelection));
        entriesBand.add(new LargeButtonAction("Delete", "Deletes the selected image(s)", UIUtils.getIcon32FromResources("actions/delete.png"), listPanel::deleteSelection));

        ribbon.rebuildRibbon();
    }
}
