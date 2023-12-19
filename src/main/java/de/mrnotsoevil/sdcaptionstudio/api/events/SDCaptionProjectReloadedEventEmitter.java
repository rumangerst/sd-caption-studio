package de.mrnotsoevil.sdcaptionstudio.api.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionProjectReloadedEventEmitter extends JIPipeEventEmitter<SDCaptionProjectReloadedEvent, SDCaptionProjectReloadedEventListener> {
    @Override
    protected void call(SDCaptionProjectReloadedEventListener listener, SDCaptionProjectReloadedEvent event) {
        listener.onProjectReloaded(event);
    }
}
