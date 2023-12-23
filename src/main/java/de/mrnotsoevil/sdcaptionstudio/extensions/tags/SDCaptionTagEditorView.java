package de.mrnotsoevil.sdcaptionstudio.extensions.tags;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.taglist.SDCaptionAutoCompleteTagListPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class SDCaptionTagEditorView extends SDCaptionProjectWorkbenchPanel {
    private final SDCaptionAutoCompleteTagListPanel listPanel;


    public SDCaptionTagEditorView(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        this.listPanel = new SDCaptionAutoCompleteTagListPanel(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Ribbon ribbon = new Ribbon(3);
        initializeRibbon(ribbon);
        add(ribbon, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);
    }

    private void initializeRibbon(Ribbon ribbon) {
        Ribbon.Task generalTask = ribbon.addTask("General");

        Ribbon.Band selectionBand = generalTask.addBand("Selection");
        selectionBand.add(new LargeButtonAction("Select all", "Selects all images", UIUtils.getIcon32FromResources("actions/stock_select-all.png"), listPanel::selectAll));
        selectionBand.add(new SmallButtonAction("Clear selection", "Clears the selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), listPanel::selectNone));
        selectionBand.add(new SmallButtonAction("Invert selection", "Inverts the selection", UIUtils.getIconFromResources("actions/edit-select-invert.png"), listPanel::invertSelection));

        Ribbon.Band entriesBand = generalTask.addBand("Entries");
//        entriesBand.add(new LargeButtonAction("Rename", "Renames the selected image(s)", UIUtils.getIcon32FromResources("actions/edit-rename.png"), listPanel::renameSelection));
        entriesBand.add(new LargeButtonAction("Delete", "Deletes the selected tags(s)", UIUtils.getIcon32FromResources("actions/delete.png"), listPanel::deleteSelection));
        entriesBand.add(new LargeButtonAction("Clear", "Deletes all tags", UIUtils.getIcon32FromResources("actions/rabbitvcs-clear.png"), listPanel::clearTags));

        Ribbon.Task importTask = ribbon.addTask("Import");
        Ribbon.Band fromFilesBand = importTask.addBand("From files");
        fromFilesBand.add(new LargeButtonAction("From *.csv", "Imports tags from a standard CSV file (Same format as a1111-sd-webui-tagcomplete)", UIUtils.getIcon32FromResources("actions/document-import.png"), this::importCSV));
        Ribbon.Band presetsBand = importTask.addBand("Presets");
        initializeImportFromPresetsBand(presetsBand);

        ribbon.rebuildRibbon();
    }

    private void initializeImportFromPresetsBand(Ribbon.Band presetsBand) {
        for (String resourceFile : SDCaptionUtils.walkInternalResourceFolder("tags")) {
            if(resourceFile.endsWith(".csv")) {
                String fileName = resourceFile.substring(resourceFile.lastIndexOf("/") + 1);
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                presetsBand.add(new LargeButtonAction(fileName, "Loads a preset from " + fileName, UIUtils.getIcon32FromResources("actions/quickopen-file.png"), () -> {
                    importCSVFromResources(resourceFile);
                }));
            }
        }

    }

    private void importCSVFromResources(String resourceFile) {
        try {
            String fileName = resourceFile.substring(resourceFile.lastIndexOf("/") + 1);
           int count = getProject().importTagsFromString(Resources.toString(SDCaptionUtils.class.getResource(resourceFile), Charsets.UTF_8), fileName);
           JOptionPane.showMessageDialog(this, "Imported " + count + " tags.", "Import tags", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void importCSV() {
        Path csvPath = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.External, "Import tags *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if(csvPath != null) {
            int count = getProject().importTagsFromFile(csvPath);
            JOptionPane.showMessageDialog(this, "Imported " + count + " tags.", "Import tags", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
