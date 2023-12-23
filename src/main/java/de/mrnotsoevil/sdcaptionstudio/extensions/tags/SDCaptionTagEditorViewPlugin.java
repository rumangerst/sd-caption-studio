package de.mrnotsoevil.sdcaptionstudio.extensions.tags;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionEditorPlugin;
import de.mrnotsoevil.sdcaptionstudio.extensions.singleimage.SDCaptionSingleImageView;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin(type = SDCaptionEditorPlugin.class, priority = Priority.LOW)
public class SDCaptionTagEditorViewPlugin implements SDCaptionEditorPlugin {
    @Override
    public String getEditorName() {
        return "Autocomplete";
    }

    @Override
    public String getEditorDescription() {
        return "Allows to import/manage tags for the auto-complete function";
    }

    @Override
    public Icon getEditorIcon() {
        return UIUtils.getIconFromResources("actions/tag.png");
    }

    @Override
    public SDCaptionProjectWorkbenchPanel createEditor(SDCaptionProjectWorkbench workbench) {
        return new SDCaptionTagEditorView(workbench);
    }
}
