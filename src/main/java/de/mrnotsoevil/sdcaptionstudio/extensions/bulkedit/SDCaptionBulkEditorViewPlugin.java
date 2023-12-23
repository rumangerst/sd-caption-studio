package de.mrnotsoevil.sdcaptionstudio.extensions.bulkedit;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionEditorPlugin;
import de.mrnotsoevil.sdcaptionstudio.extensions.tags.SDCaptionTagEditorView;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin(type = SDCaptionEditorPlugin.class, priority = Priority.HIGH)
public class SDCaptionBulkEditorViewPlugin implements SDCaptionEditorPlugin {
    @Override
    public String getEditorName() {
        return "Bulk edit";
    }

    @Override
    public String getEditorDescription() {
        return "Edit all/a selection of images at once";
    }

    @Override
    public Icon getEditorIcon() {
        return UIUtils.getIconFromResources("actions/run-build.png");
    }

    @Override
    public SDCaptionProjectWorkbenchPanel createEditor(SDCaptionProjectWorkbench workbench) {
        return new SDCaptionBulkEditorView(workbench);
    }
}
