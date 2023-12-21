package de.mrnotsoevil.sdcaptionstudio.ui.templatemanager;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionTemplate;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class SDCaptionTempleCellRenderer extends JPanel implements ListCellRenderer<SDCaptionTemplate> {
    private final SDCaptionProject project;
    private final SolidColorIcon icon = new SolidColorIcon(8, 48);
    private JLabel nameLabel;
    private JLabel captionLabel;
    private JLabel accessorLabel;

    public SDCaptionTempleCellRenderer(SDCaptionProject project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        Insets defaultInsets = new Insets(2,2,2,2);
        setLayout(new GridBagLayout());
        nameLabel = new JLabel();
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        captionLabel = new JLabel();
        captionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        accessorLabel = new JLabel();
        accessorLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

        add(new JLabel(icon), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                fill = NONE;
                gridheight = 2;
                insets = defaultInsets;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
                insets = defaultInsets;
            }
        });
        add(accessorLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                fill = NONE;
                weightx = 0;
                insets = defaultInsets;
            }
        });
        add(captionLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
                insets = defaultInsets;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends SDCaptionTemplate> list, SDCaptionTemplate value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(StringUtils.orElse(value.getName(), value.getKey()));
            icon.setFillColor(value.getColor());
            captionLabel.setText(value.getContent());
            accessorLabel.setText("@" + value.getKey());
        }

        if (isSelected) {
            if (UIUtils.DARK_THEME) {
                setBackground(new Color(0x2A537A));
            } else {
                setBackground(UIManager.getColor("List.selectionBackground"));
            }
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }

    public SDCaptionProject getProject() {
        return project;
    }

}
