package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.SDCaptionStudio;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MemoryStatusUI;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.data.MemoryOptionsControl;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueButton;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Context;
import org.scijava.Contextual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SDCaptionWorkbench extends JFrame implements JIPipeWorkbench, Contextual {

    private final Context context;
    private final DocumentTabPane documentTabPane = new DocumentTabPane();
    private final JLabel statusText = new JLabel("Ready");
    public SDCaptionWorkbench(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {

        // Window initialization
        setTitle("SD Caption Studio");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(SDCaptionStudio.getInstance().getApplicationIcon().getImage());
        setSize(1024, 768);
        UIUtils.setToAskOnClose(this, () -> "Do you really want to close SD Caption Studio?", "Close window");

        // Tab pane and projects
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(documentTabPane, BorderLayout.CENTER);

        // Toolbar
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorderPainted(false);
        initializeMenuBar(menuBar);
        panel.add(menuBar, BorderLayout.NORTH);

        // Status bar
        initializeStatusBar(panel);

        // init welcome panel
        documentTabPane.registerSingletonTab("WELCOME",
                "Getting started",
                UIUtils.getIconFromResources("actions/help-info.png"),
                () -> new CustomWelcomePanel(this),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                DocumentTabPane.SingletonTabMode.Present);

        setContentPane(panel);
    }

    private void initializeMenuBar(JMenuBar menuBar) {
        JMenu projectMenu = new JMenu("Project");

        JMenuItem newItem = UIUtils.createMenuItem("New", "Creates a new directory",UIUtils.getIconFromResources("actions/document-new.png"), this::newProject);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(newItem);

        JMenuItem openItem = UIUtils.createMenuItem("Open", "Opens a new directory",UIUtils.getIconFromResources("actions/project-open.png"), this::openDirectory);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(openItem);

        projectMenu.add(new CustomRecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("actions/clock.png"), this));

        projectMenu.addSeparator();

        JMenuItem saveCurrentItem = UIUtils.createMenuItem("Save (current)", "Saves the currently open tab",UIUtils.getIconFromResources("actions/save.png"), this::saveCurrent);
        saveCurrentItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        projectMenu.add(saveCurrentItem);

        JMenuItem saveAllItem = UIUtils.createMenuItem("Save (all tabs)", "Saves the all opened tabs",UIUtils.getIconFromResources("actions/save.png"), this::saveAll);
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
    }

    private void saveAll() {
        for (SDCaptionProject openProject : getOpenProjects()) {
            openProject.saveAll();
        }
        sendStatusBarText("Saved all currently open projects");
    }

    private void saveCurrent() {
        if(documentTabPane.getCurrentContent() instanceof SDCaptionProjectUI) {
            ((SDCaptionProjectUI) documentTabPane.getCurrentContent()).saveAll();
        }
        else {
            sendStatusBarText("Current tab is not a project");
        }
    }

    private void newProject() {
        openDirectory();
    }

    private void initializeStatusBar(JPanel panel) {
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusBar.add(statusText);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        statusBar.add(new MemoryStatusUI());
        panel.add(statusBar, BorderLayout.SOUTH);
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

    public void openDirectory() {
        Path directory = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Projects, "Open image directory");
        if(directory != null) {
            openDirectory(directory);
        }
    }

    public List<SDCaptionProject> getOpenProjects() {
        List<SDCaptionProject> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if(tab.getContent() instanceof SDCaptionProjectUI) {
                result.add(((SDCaptionProjectUI) tab.getContent()).getProject());
            }
        }
        return result;
    }

    public void openDirectory(Path directory) {
        // Find already open one
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if(tab.getContent() instanceof SDCaptionProjectUI) {
                if(Objects.equals(directory, ((SDCaptionProjectUI) tab.getContent()).getProject().getStoragePath())) {
                    documentTabPane.switchToTab(tab);
                    sendStatusBarText("Switched back to existing tab");
                    return;
                }
            }
        }

        // Open new tab
        int option = JOptionPane.showOptionDialog(this,
                "Are the images only in the selected directory or can they be in sub-directories?",
                "Open project",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Yes (include subdirectories)", "No (only selected directory)", "Cancel"},
                "Yes (include subdirectories)");

        if(option == JOptionPane.CANCEL_OPTION) {
            return;
        }

        SDCaptionProject project = new SDCaptionProject(directory, option == JOptionPane.YES_NO_OPTION ? Integer.MAX_VALUE : 2);
        documentTabPane.addTab(directory.getFileName().toString(), UIUtils.getIconFromResources("actions/virtual-desktops.png"),
                new SDCaptionProjectUI(this, project), DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        ProjectsSettings.getInstance().addRecentProject(directory);
        sendStatusBarText("Opened " + directory);

        documentTabPane.switchToLastTab();
    }

    @Override
    public void dispose() {

        // Known issue with Scijava
        JIPipe.exitLater(0);

        super.dispose();
    }

    @Override
    public Window getWindow() {
        return this;
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
}
