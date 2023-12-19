package de.mrnotsoevil.sdcaptionstudio.extensions.singleimage;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionEditorPlugin;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin(type = SDCaptionEditorPlugin.class, priority = Priority.FIRST)
public class SDCaptionSingleImageCaptionEditorPlugin implements SDCaptionEditorPlugin {
    @Override
    public String getEditorName() {
        return "Single image";
    }

    @Override
    public String getEditorDescription() {
        return "Allows to caption image-by-image";
    }

    @Override
    public Icon getEditorIcon() {
        return UIUtils.getIconFromResources("actions/virtual-desktops.png");
    }

    @Override
    public SDCaptionProjectWorkbenchPanel createEditor(SDCaptionProjectWorkbench workbench) {
        return new SDCaptionSingleImageCaptionEditor(workbench);
    }
}
