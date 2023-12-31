package de.mrnotsoevil.sdcaptionstudio.api.events;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

public class SDCaptionProjectReloadedEvent extends AbstractJIPipeEvent {
    private final SDCaptionProject project;

    public SDCaptionProjectReloadedEvent(SDCaptionProject project) {
        super(project);
        this.project = project;
    }

    public SDCaptionProject getProject() {
        return project;
    }
}
