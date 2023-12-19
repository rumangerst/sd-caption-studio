package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import ij.IJ;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SDCaptionProjectUI extends JIPipeWorkbenchPanel implements Disposable {
    private final SDCaptionProject project;
    private final JPanel propertiesPanel = new JPanel(new BorderLayout());
    private final JToggleButton autoSaveButton = new JToggleButton("Auto-save", UIUtils.getIconFromResources("actions/view-refresh.png"), true);
    private final SDCaptionProjectFilesUI projectFilesPanel;
    private final SDCaptionEditorUI captionEditorUI;

    public SDCaptionProjectUI(JIPipeWorkbench workbench, SDCaptionProject project) {
        super(workbench);
        this.project = project;
        this.projectFilesPanel = new SDCaptionProjectFilesUI(this);
        captionEditorUI = new SDCaptionEditorUI(this);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                projectFilesPanel,
                propertiesPanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(400, true));
        add(splitPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        JButton saveButton = UIUtils.createButton("Save", UIUtils.getIconFromResources("actions/save.png"), this::saveAll);
        saveButton.setToolTipText("Saves all edited captions");
        toolBar.add(saveButton);

        autoSaveButton.setToolTipText("If enabled, automatically save all changes to captions");
        toolBar.add(autoSaveButton);

        toolBar.addSeparator();

        JButton dumpButton = UIUtils.createButton("Emergency dump", UIUtils.getIconFromResources("actions/document-export.png"), this::emergencyDump);
        dumpButton.setToolTipText("Use this function if the directory structure was messed up by a script or other tool.\n" +
               "Dumps all captions into a dedicated directory.");
        toolBar.add(dumpButton);

        toolBar.add(UIUtils.createButton("Reset", UIUtils.getIconFromResources("actions/edit-undo.png"), this::resetProject));

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Open directory", UIUtils.getIconFromResources("actions/project-open.png"), this::openProjectDirectory));

        initializeProperties();
    }

    private void initializeProperties() {
        propertiesPanel.setLayout(new BorderLayout());
        propertiesPanel.add(captionEditorUI, BorderLayout.CENTER);
    }

    private void resetProject() {
        if(JOptionPane.showConfirmDialog(this, "Do you really want to reset this project?\n" +
                "Please note that this will not help if you have auto-save enabled.",
                "Reset project",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_NO_OPTION) {
            project.reset();
            projectFilesPanel.reload();
        }
    }

    private void emergencyDump() {

    }

    private void openProjectDirectory() {
        try {
            Desktop.getDesktop().open(project.getStoragePath().toFile());
        } catch (IOException e) {
            IJ.handleException(e);
        }
    }

    public void saveAll() {
        project.saveAll();
        getWorkbench().sendStatusBarText("Saved all captions");
    }

    public SDCaptionProject getProject() {
        return project;
    }

    @Override
    public void dispose() {
        project.dispose();
    }

    public void editCaption(SDCaptionedImage value) {
        if(value != null) {
            captionEditorUI.editCaption(value);
        }
    }
}
