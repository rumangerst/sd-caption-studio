package de.mrnotsoevil.sdcaptionstudio.api.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionProjectTemplatesChangedEventEmitter extends JIPipeEventEmitter<SDCaptionProjectTemplatesChangedEvent,
        SDCaptionProjectTemplatesChangedEventListener> {
    @Override
    protected void call(SDCaptionProjectTemplatesChangedEventListener listener, SDCaptionProjectTemplatesChangedEvent event) {
        listener.onProjectTemplatesChanged(event);
    }
}
