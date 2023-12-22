package de.mrnotsoevil.sdcaptionstudio.api.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionedImagePropertyUpdatedEventEmitter extends JIPipeEventEmitter<SDCaptionedImagePropertyUpdatedEvent, SDCaptionedImagePropertyUpdatedEventListener> {
    @Override
    protected void call(SDCaptionedImagePropertyUpdatedEventListener listener, SDCaptionedImagePropertyUpdatedEvent event) {
        listener.onCaptionedImagePropertyUpdated(event);
    }
}
