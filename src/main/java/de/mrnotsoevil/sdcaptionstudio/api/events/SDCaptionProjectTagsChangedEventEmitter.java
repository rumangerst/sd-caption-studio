package de.mrnotsoevil.sdcaptionstudio.api.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionProjectTagsChangedEventEmitter extends JIPipeEventEmitter<SDCaptionProjectTagsChangedEvent, SDCaptionProjectTagsChangedEventListener> {
    @Override
    protected void call(SDCaptionProjectTagsChangedEventListener listener, SDCaptionProjectTagsChangedEvent event) {
        listener.onProjectTagsChanged(event);
    }
}
