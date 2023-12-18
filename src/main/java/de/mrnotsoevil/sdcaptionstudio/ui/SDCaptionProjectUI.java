package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.scijava.Disposable;

public class SDCaptionProjectUI extends JIPipeWorkbenchPanel implements Disposable {
    private final SDCaptionProject project;
    public SDCaptionProjectUI(JIPipeWorkbench workbench, SDCaptionProject project) {
        super(workbench);
        this.project = project;
    }

    public SDCaptionProject getProject() {
        return project;
    }

    @Override
    public void dispose() {
        project.dispose();
    }
}
