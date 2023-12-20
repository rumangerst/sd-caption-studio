package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionEditorPlugin;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionRecentProjectsMenu;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MemoryStatusUI;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.notifications.NotificationButton;
import org.hkijena.jipipe.ui.notifications.WorkbenchNotificationInboxUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueButton;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SDCaptionProjectWorkbench extends JPanel implements JIPipeWorkbench, Contextual {

    public static final String TAB_NOTIFICATIONS = "NOTIFICATIONS";
    private final SDCaptionProjectWindow window;
    private final Context context;
    private final DocumentTabPane documentTabPane = new DocumentTabPane();
    private final JLabel statusText = new JLabel("Ready");
    private final SDCaptionProject project;
    private final JMenuBar menuBar = new JMenuBar();

    public SDCaptionProjectWorkbench(SDCaptionProjectWindow window, Context context, SDCaptionProject project) {
        this.window = window;
        this.context = context;
        this.project = project;

        // Fully resets the project and loads all images
        project.reset();

        // Initialize
        initialize();
        sendStatusBarText("Loaded/Created project " + project.getProjectFilePath());
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Toolbar

        menuBar.setBorderPainted(false);
        initializeMenuBar(menuBar);
        add(menuBar, BorderLayout.NORTH);

        // Status bar
        initializeStatusBar();
        add(documentTabPane, BorderLayout.CENTER);

        initializeSingletonTabs();
        initializeEditors();
    }

    private void initializeSingletonTabs() {
        documentTabPane.registerSingletonTab(TAB_NOTIFICATIONS,
                "Notifications",
                UIUtils.getIconFromResources("emblems/warning.png"),
                () -> new WorkbenchNotificationInboxUI(this),
                DocumentTabPane.SingletonTabMode.Hidden);
    }

    private void initializeEditors() {
        JMenu editorsMenu = new JMenu("Edit");
        menuBar.add(editorsMenu, 1);

        PluginService pluginService = getContext().getService(PluginService.class);
        List<PluginInfo<SDCaptionEditorPlugin>> pluginList = pluginService.getPluginsOfType(SDCaptionEditorPlugin.class).stream()
                .sorted(JIPipe::comparePlugins).collect(Collectors.toList());

        for (PluginInfo<SDCaptionEditorPlugin> pluginInfo : pluginList) {
            String identifier = pluginInfo.getIdentifier();
            try {
                SDCaptionEditorPlugin plugin = pluginInfo.createInstance();
                SDCaptionProjectWorkbenchPanel editor = plugin.createEditor(this);
                documentTabPane.registerSingletonTab("TAB_EDITOR_" + identifier,
                        plugin.getEditorName(),
                        plugin.getEditorIcon(),
                        () -> editor,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
                editorsMenu.add(UIUtils.createMenuItem(plugin.getEditorName(),
                        plugin.getEditorDescription(),
                        plugin.getEditorIcon(),
                        () -> documentTabPane.selectSingletonTab("TAB_EDITOR_" + identifier)));
            } catch (InstantiableException e) {
                IJ.handleException(e);
                JIPipeNotification notification = new JIPipeNotification("editor-init-error-" + identifier);
                notification.setHeading("Unable to load editor");
                notification.setDescription("The editor component for " + pluginInfo + " could not be loaded. Please contact the plugin author.");
                getNotificationInbox().push(notification);
            }
        }
    }

    public SDCaptionProject getProject() {
        return project;
    }

    private void initializeMenuBar(JMenuBar menuBar) {
        JMenu projectMenu = new JMenu("Project");

        JMenuItem openDirectoryItem = UIUtils.createMenuItem("Open directory ...", "Opens an existing directory",UIUtils.getIconFromResources("actions/project-open.png"), this::openDirectory);
        openDirectoryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(openDirectoryItem);

        JMenuItem openProjectItem = UIUtils.createMenuItem("Open project ...", "Opens an existing project",UIUtils.getIconFromResources("actions/project-open.png"), this::openProject);
        openProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(openProjectItem);

        projectMenu.add(new SDCaptionRecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("actions/clock.png"), this));

        projectMenu.addSeparator();

        JMenuItem saveCurrentItem = UIUtils.createMenuItem("Save ...", "Saves the currently open tab",UIUtils.getIconFromResources("actions/save.png"), this::saveProject);
        saveCurrentItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(saveCurrentItem);

        JMenuItem saveAllItem = UIUtils.createMenuItem("Save as ...", "Saves the all opened tabs",UIUtils.getIconFromResources("actions/save.png"), this::saveProjectAs);
        saveAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        projectMenu.add(saveAllItem);

        projectMenu.addSeparator();

        JMenuItem settingsItem = new JMenuItem("Application settings", UIUtils.getIconFromResources("actions/configure.png"));
        settingsItem.setToolTipText("Opens the application settings");
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        settingsItem.addActionListener(e -> openSettings());
        projectMenu.add(settingsItem);

        projectMenu.addSeparator();

        JMenuItem exitButton = new JMenuItem("Exit", UIUtils.getIconFromResources("actions/exit.png"));
        exitButton.addActionListener(e -> getWindow().dispatchEvent(new WindowEvent(getWindow(), WindowEvent.WINDOW_CLOSING)));
        projectMenu.add(exitButton);

        menuBar.add(projectMenu);


        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(new JIPipeRunnerQueueButton(this));
        menuBar.add(new NotificationButton(this));
    }

    private void openDirectory() {
        Path directory = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Projects, "Open image directory");
        if(directory != null) {
            window.openProject(directory, false);
        }
    }

    private void saveProjectAs() {
        project.commitAll();
        window.saveProjectAs(false);
    }

    private void saveProject() {
        project.commitAll();
        window.saveProjectAs(true);
    }

    private void newProject() {
        openProject();
    }

    private void initializeStatusBar() {
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusBar.add(statusText);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        statusBar.add(new MemoryStatusUI());
        add(statusBar, BorderLayout.SOUTH);
    }

    private void openSettings() {
        JDialog dialog = new JDialog(getWindow());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setTitle("SD Caption Studio - Application settings");
        dialog.setModal(true);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeApplicationSettingsUI applicationSettingsUI = new JIPipeApplicationSettingsUI(this);
        UIUtils.addEscapeListener(dialog);
        JPanel contentPanel = new JPanel(new BorderLayout(8,8));
        contentPanel.add(applicationSettingsUI, BorderLayout.CENTER);

        AtomicBoolean saved = new AtomicBoolean(false);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if(!saved.get()) {
                    JIPipe.getSettings().reload();
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton resetButton = new JButton("Reset", UIUtils.getIconFromResources("actions/edit-reset.png"));
        resetButton.addActionListener(e -> {
            JIPipe.getSettings().reload();
            applicationSettingsUI.selectNode("/General");
        });
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            JIPipe.getSettings().reload();
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);
        JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
        saveButton.addActionListener(e -> {
            JIPipe.getSettings().save();
            saved.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setSize(1280,768);
        dialog.setLocationRelativeTo(getWindow());
        dialog.setVisible(true);
    }

    public void openProject() {
        Path projectFile = FileChooserSettings.openFile(this,
                FileChooserSettings.LastDirectoryKey.Projects,
                "Open project file",
                UIUtils.EXTENSION_FILTER_JSON);
        if(projectFile != null) {
            window.openProject(projectFile, false);
        }
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public void sendStatusBarText(String text) {
        LocalDateTime localDateTime = LocalDateTime.now();
        statusText.setText(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + text);
    }

    @Override
    public boolean isProjectModified() {
        return false;
    }

    @Override
    public void setProjectModified(boolean modified) {

    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public DocumentTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return JIPipeNotificationInbox.getInstance();
    }

    public void unload() {

    }
}
