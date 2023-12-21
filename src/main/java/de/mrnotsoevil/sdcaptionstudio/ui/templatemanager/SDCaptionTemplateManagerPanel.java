package de.mrnotsoevil.sdcaptionstudio.ui.templatemanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionTemplate;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import de.mrnotsoevil.sdcaptionstudio.ui.utils.SDCaptionUtils;
import ij.IJ;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SDCaptionTemplateManagerPanel extends SDCaptionProjectWorkbenchPanel implements SDCaptionProjectTemplatesChangedEventListener {
    private final JToolBar toolBar = new JToolBar();
    private final JList<SDCaptionTemplate> templateJList = new JList<>();
    private final SearchTextField searchTextField = new SearchTextField();

    public SDCaptionTemplateManagerPanel(SDCaptionProjectWorkbench workbench) {
        super(workbench);
        initialize();
        reload();

        getProject().getProjectTemplatesChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        toolBar.setFloatable(false);

        toolBar.add(searchTextField);
        searchTextField.addActionListener(e -> reload());

        JButton manageButton = new JButton("Manage ...", UIUtils.getIconFromResources("actions/wrench.png"));
        JPopupMenu manageMenu = UIUtils.addPopupMenuToButton(manageButton);
        createContextMenu(manageMenu);

        toolBar.add(manageButton);

        templateJList.setCellRenderer(new SDCaptionTempleCellRenderer(getProject()));
        templateJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int i = templateJList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        templateJList.addSelectionInterval(i, i);
                    }
                    JPopupMenu menu = new JPopupMenu();
                    createContextMenu(menu);
                    menu.show(templateJList, e.getX(), e.getY());
                }
            }
        });

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(templateJList), BorderLayout.CENTER);
    }

    private void createContextMenu(JPopupMenu target) {
        if (templateJList.getSelectedValue() != null) {
            target.add(UIUtils.createMenuItem("Edit ...", "Edit the selected template",
                    UIUtils.getIconFromResources("actions/edit.png"),
                    () -> editTemplate(templateJList.getSelectedValue())));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("New ...",
                    "Create a new template",
                    UIUtils.getIconFromResources("actions/document-new.png"),
                    () -> createNewTemplate("INSERT CONTENT HERE")));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("Copy", "Copies the templates into the clipboard",
                    UIUtils.getIconFromResources("actions/edit-copy.png"), this::copyTemplatesToClipboard));
            target.add(UIUtils.createMenuItem("Paste", "Imports templates from clipboard",
                    UIUtils.getIconFromResources("actions/edit-paste.png"), this::importTemplatesFromClipboard));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("Recolor ...", "Sets the color of the selected templates",
                    UIUtils.getIconFromResources("actions/color-picker.png"), this::recolorTemplates));
            target.add(UIUtils.createMenuItem("Delete ...", "Deletes the selected templates",
                    UIUtils.getIconFromResources("actions/delete.png"), this::deleteTemplates));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("Import from *.json ...", "Imports the selected templates from a JSON file",
                    UIUtils.getIconFromResources("actions/document-import.png"), this::importTemplatesFromJson));
            target.add(UIUtils.createMenuItem("Export to *.json ...", "Exports the selected templates as JSON file",
                    UIUtils.getIconFromResources("actions/document-export.png"), this::exportTemplatesFromJson));

        } else {
            target.add(UIUtils.createMenuItem("New ...",
                    "Create a new template",
                    UIUtils.getIconFromResources("actions/document-new.png"),
                    () -> createNewTemplate("INSERT CONTENT HERE")));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("Paste", "Imports templates from clipboard",
                    UIUtils.getIconFromResources("actions/edit-paste.png"), this::importTemplatesFromClipboard));
            target.addSeparator();
            target.add(UIUtils.createMenuItem("Import from *.json ...", "Imports the selected templates from a JSON file",
                    UIUtils.getIconFromResources("actions/document-import.png"), this::importTemplatesFromJson));
        }

    }

    private void deleteTemplates() {
        List<SDCaptionTemplate> valuesList = ImmutableList.copyOf(templateJList.getSelectedValuesList());
        if (!valuesList.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the following templates?\n\n" +
                            valuesList.stream().map(SDCaptionTemplate::getDisplayName).collect(Collectors.joining("\n")),
                    "Delete selected templates",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                List<String> keys = valuesList.stream().map(SDCaptionTemplate::getKey).collect(Collectors.toList());
                for (String key : keys) {
                    getProject().removeTemplate(key);
                }
                getWorkbench().sendStatusBarText("Deleted " + valuesList.size() + " templates");
            }
        }
    }

    private void importTemplatesFromClipboard() {
        try {
            TypeReference<List<SDCaptionTemplate>> typeReference = new TypeReference<List<SDCaptionTemplate>>() {
            };
            String data = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            List<SDCaptionTemplate> templates = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(data);
            importTemplates(templates);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void importTemplatesFromJson() {
        Path path = FileChooserSettings.openFile(getProjectWorkbench().getWindow(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Import templates",
                UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            TypeReference<List<SDCaptionTemplate>> typeReference = new TypeReference<List<SDCaptionTemplate>>() {
            };
            try {
                List<SDCaptionTemplate> templates = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(path.toFile());
                importTemplates(templates);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private void importTemplates(List<SDCaptionTemplate> templates) {
        int result = 0;
        int importedCount = 0;
        JCheckBox applyAll = new JCheckBox("Apply to all");
        for (SDCaptionTemplate template : templates) {
            if (getProject().getTemplates().containsKey(template.getKey())) {
                if (!applyAll.isSelected()) {
                    result = JOptionPane.showOptionDialog(this,
                            UIUtils.boxVertical(new JLabel("The following template is already present in the project: " +
                                            template.getDisplayName() + "\n\nWhat should be done?"),
                                    applyAll),
                            "Import templates",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new Object[]{"Overwrite", "Rename", "Skip"},
                            "Overwrite");
                }
            }
            switch (result) {
                case 0: {
                    // Overwrite
                    getProject().addTemplate(template, true);
                    ++importedCount;
                }
                break;
                case 1: {
                    // Rename
                    getProject().addTemplate(template, false);
                    ++importedCount;
                }
                break;
            }
        }

        getWorkbench().sendStatusBarText("Imported " + importedCount + " templates");
    }

    private void copyTemplatesToClipboard() {
        List<SDCaptionTemplate> valuesList = templateJList.getSelectedValuesList();
        if (!valuesList.isEmpty()) {
            UIUtils.copyToClipboard(JsonUtils.toPrettyJsonString(valuesList));
            getWorkbench().sendStatusBarText("Copied templates into the clipboard");
        }
    }

    private void exportTemplatesFromJson() {
        List<SDCaptionTemplate> valuesList = templateJList.getSelectedValuesList();
        if (!valuesList.isEmpty()) {
            Path path = FileChooserSettings.saveFile(getProjectWorkbench().getWindow(),
                    FileChooserSettings.LastDirectoryKey.Data,
                    "Export templates",
                    UIUtils.EXTENSION_FILTER_JSON);
            if (path != null) {
                JsonUtils.saveToFile(valuesList, path);
            }
        }
    }

    private void recolorTemplates() {
        List<SDCaptionTemplate> valuesList = templateJList.getSelectedValuesList();
        if (!valuesList.isEmpty()) {
            Color value = JColorChooser.showDialog(this, "Recolor templates", valuesList.get(0).getColor());
            if (value != null) {
                for (SDCaptionTemplate template : valuesList) {
                    template.setColor(value);
                }
                templateJList.repaint();
            }
        }
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public JList<SDCaptionTemplate> getTemplateJList() {
        return templateJList;
    }

    private void reload() {
        DefaultListModel<SDCaptionTemplate> model = new DefaultListModel<>();
        for (Map.Entry<String, SDCaptionTemplate> entry : getProject().getTemplates().entrySet().stream().sorted(
                Map.Entry.comparingByKey(NaturalOrderComparator.INSTANCE)
        ).collect(Collectors.toList())) {
            if (searchTextField.test("@" + entry.getKey() + " " + StringUtils.nullToEmpty(entry.getValue().getName()) + " " +
                    entry.getValue().getDescription().getBody())) {
                model.addElement(entry.getValue());
            }
        }
        templateJList.setModel(model);
    }

    @Override
    public void onProjectTemplatesChanged(SDCaptionProjectTemplatesChangedEvent event) {
        reload();
    }

    public void createNewTemplate(String content) {
        if (SDCaptionUtils.isValidTemplateContent(content)) {

            SDCaptionTemplate template = new SDCaptionTemplate();
            template.setContent(content);

            while (true) {
                boolean result = ParameterPanel.showDialog(getProjectWorkbench(), template,
                        SDCaptionUtils.getDocumentation("create-templates.md"),
                        "Create template",
                        ParameterPanel.DEFAULT_DIALOG_FLAGS);
                if (result) {
                    String key = StringUtils.nullToEmpty(template.getKey()).trim();
                    if (StringUtils.isNullOrEmpty(key)) {
                        JOptionPane.showMessageDialog(this,
                                "The key cannot be empty!",
                                "Create template",
                                JOptionPane.ERROR_MESSAGE);
                    } else if (getProject().getTemplates().containsKey(key)) {
                        JOptionPane.showMessageDialog(this,
                                "The key already exists!",
                                "Create template",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        String validKey = SDCaptionUtils.toValidTemplateKey(key);
                        if (!validKey.equals(key)) {
                            template.setKey(validKey);
                            JOptionPane.showMessageDialog(this,
                                    "The key was not valid and edited to conform to the required format.",
                                    "Create template",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            // Everything OK
                            SDCaptionTemplate newTemplate = getProject().createTemplate(template.getKey(), template.getContent());
                            newTemplate.copyMetadataFrom(template);
                            getProject().getProjectTemplatesChangedEventEmitter().emit(new SDCaptionProjectTemplatesChangedEvent(getProject()));
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void editTemplate(SDCaptionTemplate template) {
        if (template != null) {
            SDCaptionTemplate copy = new SDCaptionTemplate(template);
            copy.setProject(null);
            copy.setKey(template.getKey());
            while (true) {
                boolean result = ParameterPanel.showDialog(getProjectWorkbench(), copy,
                        SDCaptionUtils.getDocumentation("edit-templates.md"),
                        "Edit template",
                        ParameterPanel.DEFAULT_DIALOG_FLAGS);
                if (!result) {
                    return;
                }

                String newKey = StringUtils.nullToEmpty(copy.getKey()).trim();
                boolean contentChanged = !Objects.equals(copy.getContent(), template.getContent());

                if (StringUtils.isNullOrEmpty(newKey)) {
                    JOptionPane.showMessageDialog(this,
                            "The key cannot be empty!",
                            "Edit template",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                String validKey = SDCaptionUtils.toValidTemplateKey(newKey);
                if (!validKey.equals(newKey)) {
                    template.setKey(validKey);
                    JOptionPane.showMessageDialog(this,
                            "The key was not valid and edited to conform to the required format.",
                            "Edit template",
                            JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }
                if (getProject().getTemplates().containsKey(newKey)) {
                    SDCaptionTemplate existing = getProject().getTemplates().get(newKey);
                    if (existing != template) {
                        JOptionPane.showMessageDialog(this,
                                "The key already exists!",
                                "Edit template",
                                JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                }

                getProject().addTemplate(copy, true);

                if(contentChanged) {
                    for (SDCaptionedImage image : getProject().getImages().values()) {
                        if(image.getUserCaption().contains("@" + copy.getKey())) {
                            image.setUserCaptionEdited(true);
                        }
                    }
                }
                break;
            }
        }
    }
}
