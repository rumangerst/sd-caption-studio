package de.mrnotsoevil.sdcaptionstudio.ui.taglist;

import com.google.common.primitives.Ints;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionAutocompleteTag;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTagsChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTagsChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SDCaptionAutoCompleteTagListPanel extends SDCaptionProjectWorkbenchPanel implements SDCaptionProjectTagsChangedEventListener {

    private final JTable tagTable = new JTable();
    private final SearchTextField searchTextField = new SearchTextField();
    public SDCaptionAutoCompleteTagListPanel(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        initialize();
        reload();
        getProject().getProjectTagsChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(searchTextField);
        searchTextField.addActionListener(e -> reload());
        add(toolBar, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(tagTable.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(tagTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    public void reload() {
        List<SDCaptionAutocompleteTag> list = new ArrayList<>();
        for (SDCaptionAutocompleteTag tag : getProject().getTags()) {
            if(searchTextField.test(tag.getKey() + " " + StringUtils.nullToEmpty(tag.getReplacement())  + " " + StringUtils.nullToEmpty(tag.getSource()))) {
                list.add(tag);
            }
        }
        list.sort(Comparator.comparing(SDCaptionAutocompleteTag::getKey));

        tagTable.setModel(new SDCaptionAutoCompleteTagListTableModel(list));
    }

    public void selectAll() {
        if(tagTable.getModel().getRowCount() > 0) {
            tagTable.getSelectionModel().setSelectionInterval(0, tagTable.getModel().getRowCount() - 1);
        }
    }

    public void selectNone() {
        tagTable.clearSelection();
    }

    public void invertSelection() {
        if(tagTable.getModel().getRowCount() > 0) {
            Set<Integer> selectedIndices = Arrays.stream(tagTable.getSelectedRows()).boxed().collect(Collectors.toSet());
            tagTable.clearSelection();
            Set<Integer> newSelectedIndices = new HashSet<>();
            for (int i = 0; i < tagTable.getModel().getRowCount(); i++) {
                if (!selectedIndices.contains(i))
                    newSelectedIndices.add(i);
            }
            for (int row : newSelectedIndices) {
                tagTable.getSelectionModel().addSelectionInterval(row, row);
            }
        }
    }

    public void deleteSelection() {
        if(tagTable.getSelectedRows().length > 0 && JOptionPane.showConfirmDialog(this, "Do you really want to permanently delete the " +
                        "selected tags(s)?\nTip: Use the backup function in the top-right corner to quickly create a copy of all your files.\n" +
                        "Note: Please use the 'Clear' command if you want to remove all tags. Selecting all items and removing them takes a long time.",
                "Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try(BusyCursor cursor = new BusyCursor(getProjectWorkbench().getWindow())) {
                List<SDCaptionAutocompleteTag> toRemove = new ArrayList<>();
                for (int selectedRow : tagTable.getSelectedRows()) {
                    int modelRow = tagTable.convertRowIndexToModel(selectedRow);
                    toRemove.add(((SDCaptionAutoCompleteTagListTableModel) tagTable.getModel()).getTag(modelRow));
                }
                getProject().removeTags(toRemove);
            }
        }
    }

    public void clearTags() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to permanently delete ALL tags?\n" +
                        "Tip: Use the backup function in the top-right corner to quickly create a copy of all your files.",
                "Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getProject().clearTags();
        }
    }

    @Override
    public void onProjectTagsChanged(SDCaptionProjectTagsChangedEvent event) {
        reload();
    }
}
