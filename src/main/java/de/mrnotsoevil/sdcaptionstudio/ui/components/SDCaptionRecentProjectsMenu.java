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

package de.mrnotsoevil.sdcaptionstudio.ui.components;

import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWindow;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Menu that displays recently opened {@link org.hkijena.jipipe.api.JIPipeProject}
 */
public class SDCaptionRecentProjectsMenu extends JMenu implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final SDCaptionProjectWorkbench workbench;

    /**
     * @param text            item text
     * @param icon            item icon
     * @param workbench the workbench
     */
    public SDCaptionRecentProjectsMenu(String text, Icon icon, SDCaptionProjectWorkbench workbench) {
        super(text);
        this.setIcon(icon);
        this.workbench = workbench;
        reload();
        ProjectsSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void reload() {
        removeAll();
        if (ProjectsSettings.getInstance().getRecentProjects().isEmpty()) {
            JMenuItem noProject = new JMenuItem("No recent projects");
            noProject.setEnabled(false);
            add(noProject);
        } else {
            JMenuItem searchItem = new JMenuItem("Search ...", UIUtils.getIconFromResources("actions/search.png"));
            searchItem.addActionListener(e -> openProjectSearch());
            add(searchItem);
            for (Path path : ProjectsSettings.getInstance().getRecentProjects()) {
                JMenuItem openProjectItem = new JMenuItem(path.toString());
                openProjectItem.addActionListener(e -> openProject(path));
                add(openProjectItem);
            }
        }
    }

    private void openProjectSearch() {
        JDialog dialog = new JDialog(workbench.getWindow());
        dialog.setTitle("Open project");
        SDCaptionRecentProjectsListPanel panel = new SDCaptionRecentProjectsListPanel((SDCaptionProjectWindow) workbench.getWindow());
        panel.getProjectOpenedEventEmitter().subscribeLambda((emitter, lambda) -> {
            dialog.setVisible(false);
        });
        dialog.setContentPane(panel);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(workbench);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void openProject(Path path) {
        ((SDCaptionProjectWindow)workbench.getWindow()).openProject(path, false);
    }

    /**
     * Triggered when the list should be changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("recent-projects".equals(event.getKey())) {
            reload();
        }
    }
}
