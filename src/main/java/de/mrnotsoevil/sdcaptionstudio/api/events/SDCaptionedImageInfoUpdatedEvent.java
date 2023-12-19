package de.mrnotsoevil.sdcaptionstudio.api.events;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class SDCaptionedImageInfoUpdatedEvent extends AbstractJIPipeEvent {
    private final SDCaptionedImage image;

    public SDCaptionedImageInfoUpdatedEvent(SDCaptionedImage image) {
        super(image);
        this.image = image;
    }

    public SDCaptionedImage getImage() {
        return image;
    }
}
