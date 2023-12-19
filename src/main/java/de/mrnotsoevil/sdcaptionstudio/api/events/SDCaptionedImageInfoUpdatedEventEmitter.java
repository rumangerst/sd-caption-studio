package de.mrnotsoevil.sdcaptionstudio.api.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionedImageInfoUpdatedEventEmitter extends JIPipeEventEmitter<SDCaptionedImageInfoUpdatedEvent, SDCaptionedImageInfoUpdatedEventListener> {
    @Override
    protected void call(SDCaptionedImageInfoUpdatedEventListener listener, SDCaptionedImageInfoUpdatedEvent event) {
        listener.onCaptionedImageInfoUpdated(event);
    }
}
