package de.mrnotsoevil.sdcaptionstudio.api;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.utils.ArchiveUtils;

import java.io.IOException;
import java.nio.file.Path;

public class SDCaptionBackupRun extends AbstractJIPipeRunnable {
    private final SDCaptionProject project;
    private final Path backupZip;

    public SDCaptionBackupRun(SDCaptionProject project, Path backupZip) {
        this.project = project;
        this.backupZip = backupZip;
    }

    @Override
    public String getTaskLabel() {
        return "Backup";
    }

    @Override
    public void run() {
        try {
            ArchiveUtils.zipDirectory(project.getWorkDirectory(), "", backupZip, getProgressInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
