package de.mrnotsoevil.sdcaptionstudio.ui.templatemanager;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionTemplate;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEvent;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTemplatesChangedEventListener;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionProjectWorkbench;
import de.mrnotsoevil.sdcaptionstudio.ui.components.SDCaptionProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
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
        manageMenu.add(UIUtils.createMenuItem("New ...",
                "Create a new template",
                UIUtils.getIconFromResources("actions/document-new.png"),
                this::newTemplate));
        toolBar.add(manageButton);

        templateJList.setCellRenderer(new SDCaptionTempleCellRenderer(getProject()));

        add(toolBar,BorderLayout.NORTH);
        add(new JScrollPane(templateJList), BorderLayout.CENTER);
    }

    private void newTemplate() {

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
               if(searchTextField.test("@" + entry.getKey() + " " + StringUtils.nullToEmpty(entry.getValue().getName()) + " " +
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
}
