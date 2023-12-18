package de.mrnotsoevil.sdcaptionstudio.api;

import org.scijava.Disposable;

import java.nio.file.Path;

public class SDCaptionProject implements Disposable {
    private final Path storagePath;

    public SDCaptionProject(Path storagePath) {
        this.storagePath = storagePath;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();
    }
}
