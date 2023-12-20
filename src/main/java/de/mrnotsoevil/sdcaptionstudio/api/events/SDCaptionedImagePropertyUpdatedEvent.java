package de.mrnotsoevil.sdcaptionstudio.api.events;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImageProperty;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

public class SDCaptionedImagePropertyUpdatedEvent extends AbstractJIPipeEvent {
    private final SDCaptionedImage image;
    private final SDCaptionedImageProperty property;

    public SDCaptionedImagePropertyUpdatedEvent(SDCaptionedImage image, SDCaptionedImageProperty property) {
        super(image);
        this.image = image;
        this.property = property;
    }

    public SDCaptionedImage getImage() {
        return image;
    }

    public SDCaptionedImageProperty getProperty() {
        return property;
    }
}
