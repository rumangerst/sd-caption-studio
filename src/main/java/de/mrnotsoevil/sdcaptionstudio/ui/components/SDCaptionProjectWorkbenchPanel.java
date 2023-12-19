package de.mrnotsoevil.sdcaptionstudio.ui.components;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;

public abstract class SDCaptionProjectWorkbenchPanel extends JIPipeWorkbenchPanel {
    public SDCaptionProjectWorkbenchPanel(SDCaptionProjectWorkbench workbench) {
        super(workbench);
    }

    public SDCaptionProject getProject() {
        return getProjectWorkbench().getProject();
    }

    public SDCaptionProjectWorkbench getProjectWorkbench() {
        return (SDCaptionProjectWorkbench) getWorkbench();
    }
}
