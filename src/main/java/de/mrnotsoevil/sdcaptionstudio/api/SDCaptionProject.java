package de.mrnotsoevil.sdcaptionstudio.api;

import ij.IJ;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class SDCaptionProject implements Disposable {
    private final Path storagePath;
    private final int maxDepth;
    private final Map<String, SDCaptionedImage> images = new HashMap<>();

    public SDCaptionProject(Path storagePath, int maxDepth) {
        this.storagePath = storagePath;
        this.maxDepth = maxDepth;
        reload();
    }

    public Path getStoragePath() {
        return storagePath;
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();
    }

    public Map<String, SDCaptionedImage> getImages() {
        return Collections.unmodifiableMap(images);
    }

    public void saveAll() {

    }

    public void reset() {
        images.clear();
        reload();
    }

    public void reload() {
        final FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
          UIUtils.EXTENSION_FILTER_PNG,
          UIUtils.EXTENSION_FILTER_BMP,
          UIUtils.EXTENSION_FILTER_JPEG,
          UIUtils.EXTENSION_FILTER_TIFF
        };
        try(Stream<Path> stream = Files.walk(storagePath, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach(path -> {
                if(Files.isRegularFile(path) && Arrays.stream(filters).anyMatch(predicate -> predicate.accept(path.toFile()))) {
                    String name = storagePath.relativize(path).toString();
                    name = name.substring(0, name.lastIndexOf('.'));

                    SDCaptionedImage image = images.getOrDefault(name, null);
                    if(image == null) {
                        image = new SDCaptionedImage();
                        images.put(name, image);
                    }

                    image.setName(name);
                    image.setImagePath(path);
                    image.setCaptionPath(storagePath.resolve(name + ".txt"));

                    // Load caption if not edited by user
                    if(!image.isCaptionEdited()) {
                        if(Files.isRegularFile(image.getCaptionPath())) {
                            try {
                                image.setCaption(new String(Files.readAllBytes(image.getCaptionPath()), StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                IJ.handleException(e);
                            }
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            IJ.handleException(e);
        }
    }
}
