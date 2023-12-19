package de.mrnotsoevil.sdcaptionstudio.api;

import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import javax.swing.*;

/**
 * Plugin type that allows to register additional editor components
 */
public interface SDCaptionEditorPlugin extends SciJavaPlugin {
    String getEditorName();
    String getEditorDescription();
    Icon getEditorIcon();
    SDCaptionProjectWorkbenchPanel createEditor(SDCaptionProjectWorkbench workbench);


}
