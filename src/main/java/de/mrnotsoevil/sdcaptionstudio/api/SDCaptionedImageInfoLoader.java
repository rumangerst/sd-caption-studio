package de.mrnotsoevil.sdcaptionstudio.api;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

public class SDCaptionedImageInfoLoader extends SwingWorker<SDCaptionedImageInfo, Object> {

    private final SDCaptionedImage captionedImage;
    private final int thumbnailSize;

    public SDCaptionedImageInfoLoader(SDCaptionedImage captionedImage, int thumbnailSize) {
        this.captionedImage = captionedImage;
        this.thumbnailSize = thumbnailSize;
    }

    @Override
    protected SDCaptionedImageInfo doInBackground() throws Exception {
        if (!Files.isRegularFile(captionedImage.getImagePath())) {
            throw new FileNotFoundException(captionedImage.getImagePath() + " does not exist!");
        }
        ImagePlus image = IJ.openImage(captionedImage.getImagePath().toString());
        double factorX = 1.0 * thumbnailSize / image.getWidth();
        double factorY = 1.0 * thumbnailSize / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        image.getProcessor().setInterpolationMethod(ImageProcessor.BICUBIC);
        ImageProcessor resized = image.getProcessor().resize((int) (factorX * image.getWidth()),
                (int) (factorY * image.getHeight()),
                smooth);
        SDCaptionedImageInfo imageInfo = new SDCaptionedImageInfo();
        imageInfo.setThumbnail(new ImageIcon(resized.createImage()));
        imageInfo.setSize(image.getWidth() + "x" + image.getHeight());
        return imageInfo;
    }

    @Override
    protected void done() {
        try {
            SDCaptionedImageInfo imageInfo = get();
            captionedImage.setCurrentImageInfo(imageInfo);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            SDCaptionedImageInfo imageInfo = new SDCaptionedImageInfo();
            imageInfo.setThumbnail(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            imageInfo.setSize("<Unable to load>");
            captionedImage.setCurrentImageInfo(imageInfo);
        }
    }
}
