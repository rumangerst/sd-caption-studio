/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package de.mrnotsoevil.sdcaptionstudio.ui;

import com.fasterxml.jackson.databind.JsonNode;
import de.mrnotsoevil.sdcaptionstudio.SDCaptionStudio;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionWelcomePanel;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionSplashScreen;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.events.WindowClosedEvent;
import org.hkijena.jipipe.ui.events.WindowClosedEventEmitter;
import org.hkijena.jipipe.ui.events.WindowOpenedEvent;
import org.hkijena.jipipe.ui.events.WindowOpenedEventEmitter;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Window that holds an {@link SDCaptionProjectWorkbench} instance
 */
public class SDCaptionProjectWindow extends JFrame {
    public static final WindowOpenedEventEmitter WINDOW_OPENED_EVENT_EMITTER = new WindowOpenedEventEmitter();
    public static final WindowClosedEventEmitter WINDOW_CLOSED_EVENT_EMITTER = new WindowClosedEventEmitter();
    private static final Set<SDCaptionProjectWindow> OPEN_WINDOWS = new HashSet<>();
    private final Context context;
    private SDCaptionProject project;
    private SDCaptionProjectWorkbench projectUI;
    private Path projectSavePath;
    private UUID sessionId = UUID.randomUUID();

    /**
     * @param context          context
     * @param project          The project
     */
    public SDCaptionProjectWindow(Context context, SDCaptionProject project) {
        SDCaptionSplashScreen.getInstance().hideSplash();
        this.context = context;
        OPEN_WINDOWS.add(this);
        WINDOW_OPENED_EVENT_EMITTER.emit(new WindowOpenedEvent(this));
        initialize();
        if(project != null) {
            loadProject(project);
        }
        else {
            loadWelcomePanel();
        }
    }

    private void loadWelcomePanel() {
        setContentPane(new SDCaptionWelcomePanel(this));
    }

    /**
     * Tries to find the window that belongs to the provided project
     *
     * @param project the project
     * @return the window or null if none is found
     */
    public static SDCaptionProjectWindow getWindowFor(SDCaptionProject project) {
        for (SDCaptionProjectWindow window : OPEN_WINDOWS) {
            if (window.project == project)
                return window;
        }
        return null;
    }

    /**
     * Creates a new window
     *
     * @param context          context
     * @param project          The project
     * @param isNewProject     if the project is a new empty project
     * @return The window
     */
    public static SDCaptionProjectWindow newWindow(Context context, SDCaptionProject project, boolean isNewProject) {
        SDCaptionProjectWindow frame = new SDCaptionProjectWindow(context, project);
        frame.pack();
        frame.setSize(1280, 800);
        frame.setVisible(true);
        return frame;
    }

    /**
     * @return All open project windows
     */
    public static Set<SDCaptionProjectWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8, 8));
        updateTitle();
        setIconImage(SDCaptionStudio.getInstance().getApplicationIcon().getImage());
        UIUtils.setToAskOnClose(this, () -> {
            if (projectUI != null && projectUI.isProjectModified()) {
                return "Do you really want to close SD Caption Studio?\nThere are some unsaved changes.";
            } else {
                return "Do you really want to close SD Caption Studio?";
            }
        }, "Close window");
        if (GeneralUISettings.getInstance().isMaximizeWindows()) {
            SwingUtilities.invokeLater(() -> setExtendedState(getExtendedState() | MAXIMIZED_BOTH));
        }
    }

    private void unloadProject() {
        projectUI.unload();
    }

    @Override
    public void dispose() {
        unloadProject();
        OPEN_WINDOWS.remove(this);
        WINDOW_CLOSED_EVENT_EMITTER.emit(new WindowClosedEvent(this));
        super.dispose();
    }

    /**
     * Loads a project into the window
     *
     * @param project          The project
     */
    public void loadProject(SDCaptionProject project) {
        this.project = project;
        this.projectUI = new SDCaptionProjectWorkbench(this, context, project);
        this.sessionId = UUID.randomUUID();
        setContentPane(projectUI);
        revalidate();
        repaint();
    }

    /**
     * Updates the title based on the current state
     */
    public void updateTitle() {
        if (projectUI == null) {
            setTitle("SD Caption Studio");
            return;
        }
        if (projectSavePath == null) {
            setTitle("SD Caption Studio - New project" + (projectUI.isProjectModified() ? "*" : ""));
        } else {
            setTitle("SD Caption Studio - " + projectSavePath + (projectUI.isProjectModified() ? "*" : ""));
        }
    }

    /**
     * Opens a project from a file or folder
     * Asks the user if it should replace the currently displayed project
     *
     * @param projectFileOrDirectory               JSON project file or result folder
     * @param forceCurrentWindow force open in current window
     */
    public void openProject(Path projectFileOrDirectory, boolean forceCurrentWindow) {

        if(projectUI == null) {
            // Handle the special case of a new window
            forceCurrentWindow = true;
        }

        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
        if (Files.isDirectory(projectFileOrDirectory)) {
            // Autoload from directory
            Path projectFile = projectFileOrDirectory.resolve("sd-caption-studio.project.json");
            if(Files.isRegularFile(projectFile)) {
                projectFileOrDirectory = projectFile;
            }
        }
        SDCaptionProject project = null;
        if (Files.isDirectory(projectFileOrDirectory)) {
            // Still a directory, so create a new project
            project = new SDCaptionProject();
            project.setWorkDirectory(projectFileOrDirectory);
            project.setStoragePath(projectFileOrDirectory);

        }
        else if (Files.isRegularFile(projectFileOrDirectory)) {
            // Load a project file
            JIPipeValidationReport report = new JIPipeValidationReport();

            notifications.connectDismissTo(JIPipeNotificationInbox.getInstance());
            try {
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(projectFileOrDirectory.toFile(), JsonNode.class);
                project = new SDCaptionProject();
                project.setWorkDirectory(projectFileOrDirectory.getParent());
                project.readMetadataFromJson(jsonData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!report.isValid()) {
                UIUtils.openValidityReportDialog(new JIPipeDummyWorkbench(), this, report, "Errors while loading the project",
                        "It seems that some data could be be loaded/restored. " +
                        "Please review the entries and apply the necessary changes.", false);
            }
        }

        // Load project into window
        if(project != null) {
            SDCaptionProjectWindow window;
            if (forceCurrentWindow) {
                window = this;
                loadProject(project);
            } else {
                window = openProjectInThisOrNewWindow("Open project", project, false);
            }
            if (window == null)
                return;
            window.projectSavePath = projectFileOrDirectory;
            window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
            window.updateTitle();
            ProjectsSettings.getInstance().addRecentProject(projectFileOrDirectory);
            if (!notifications.isEmpty()) {
                UIUtils.openNotificationsDialog(window.getProjectUI(),
                        this,
                        notifications,
                        "Potential issues found",
                        "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.",
                        true);
            }
            FileChooserSettings.getInstance().setLastDirectoryBy(FileChooserSettings.LastDirectoryKey.Projects, projectFileOrDirectory.getParent());
        }
        else {
            JOptionPane.showMessageDialog(this, "Unable to load or create a project!", "Open project", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a file chooser where the user can select a project file
     */
    public void openProject() {
        Path file = FileChooserSettings.openFile(this,
                FileChooserSettings.LastDirectoryKey.Projects,
                "Open SD Caption Studio project (*.json)",
                UIUtils.EXTENSION_FILTER_JSON);
        if (file != null) {
            openProject(file, false);
        }
    }

    /**
     * Saves the project
     *
     * @param avoidDialog If true, the project is stored in the last known valid output location if possible
     */
    public void saveProjectAs(boolean avoidDialog) {
        Path savePath = null;
        if (avoidDialog && projectSavePath != null)
            savePath = projectSavePath;
        if (savePath == null) {
            savePath = FileChooserSettings.saveFile(this,
                    FileChooserSettings.LastDirectoryKey.Projects,
                    "Save SD Caption Studio project (*.json)",
                    UIUtils.EXTENSION_FILTER_JSON);
            if (savePath == null)
                return;
        }

        if(Files.isDirectory(savePath)) {
            savePath = savePath.resolve("sd-caption-studio.project.json");
        }

        try {
            Path tempFile = Files.createTempFile(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().setWorkDirectory(savePath.getParent());
            getProject().saveProject(tempFile);

            // Check if the saved project can be loaded
            SDCaptionProject.loadProject(tempFile);

            // Overwrite the target file
            if (Files.exists(savePath))
                Files.delete(savePath);
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            projectSavePath = savePath;
            updateTitle();
            projectUI.setProjectModified(false);
            projectUI.sendStatusBarText("Saved project to " + savePath);
            ProjectsSettings.getInstance().addRecentProject(savePath);

            // Remove tmp file
            Files.delete(tempFile);
        } catch (IOException e) {
            UIUtils.openErrorDialog(getProjectUI(), this, new JIPipeValidationRuntimeException(e,
                    "Error during saving!",
                    "While saving the project into '" + savePath + "'. Any existing file was not changed or overwritten." + " The issue cannot be determined. Please contact the application authors.",
                    "Please check if you have write access to the temporary directory and the target directory. " +
                            "If this is the case, please contact the application authors."));
        }
    }

    /**
     * @param messageTitle     Description of the project source
     * @param project          The project
     * @param isNewProject     if the project is an empty project
     * @return The window that holds the project
     */
    private SDCaptionProjectWindow openProjectInThisOrNewWindow(String messageTitle, SDCaptionProject project, boolean isNewProject) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project, isNewProject);
        }
        return null;
    }

    /**
     * @return GUI command
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return The current project
     */
    public SDCaptionProject getProject() {
        return project;
    }

    /**
     * @return The current project UI
     */
    public SDCaptionProjectWorkbench getProjectUI() {
        return projectUI;
    }

    /**
     * @return Last known project save path
     */
    public Path getProjectSavePath() {
        return projectSavePath;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
