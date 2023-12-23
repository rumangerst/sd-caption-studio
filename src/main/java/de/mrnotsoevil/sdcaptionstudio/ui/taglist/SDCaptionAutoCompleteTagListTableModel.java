package de.mrnotsoevil.sdcaptionstudio.ui.taglist;

import com.google.common.collect.ImmutableList;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionAutocompleteTag;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.List;

public class SDCaptionAutoCompleteTagListTableModel implements TableModel {
    private final List<SDCaptionAutocompleteTag> tags;

    public SDCaptionAutoCompleteTagListTableModel(List<SDCaptionAutocompleteTag> tags) {
        this.tags = tags;
    }

    @Override
    public int getRowCount() {
        return tags.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Tag";
            case 1:
                return "Replace with";
            default:
                return "Source";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return tags.get(rowIndex).getKey();
            case 1:
                return tags.get(rowIndex).getReplacement();
            default:
                return tags.get(rowIndex).getSource();
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    @Override
    public void addTableModelListener(TableModelListener l) {

    }

    @Override
    public void removeTableModelListener(TableModelListener l) {

    }

    public SDCaptionAutocompleteTag getTag(int row) {
        return tags.get(row);
    }
}
