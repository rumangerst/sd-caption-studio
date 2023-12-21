package de.mrnotsoevil.sdcaptionstudio.ui.imagelist;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImageInfo;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class SDCaptionedImageListCellRenderer extends JPanel implements ListCellRenderer<SDCaptionedImage> {
    private final SDCaptionProject project;
    private int thumbnailSize = 64;
    private JLabel thumbnailLabel;
    private JLabel nameLabel;
    private JLabel captionLabel;
    private JLabel sizeLabel;

    public SDCaptionedImageListCellRenderer(SDCaptionProject project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Insets defaultInsets = new Insets(4, 4, 4, 4);
        setLayout(new GridBagLayout());
        thumbnailLabel = new JLabel(UIUtils.getIcon16FromResources("actions/hourglass-half.png"), JLabel.CENTER);
        thumbnailLabel.setPreferredSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setMaximumSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setMinimumSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        nameLabel = new JLabel();
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        captionLabel = new JLabel();
        captionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        sizeLabel = new JLabel();
        sizeLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        add(thumbnailLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
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
        add(sizeLabel, new GridBagConstraints() {
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
    public Component getListCellRendererComponent(JList<? extends SDCaptionedImage> list, SDCaptionedImage value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(value.getName());

            SDCaptionedImageInfo info = value.getImageInfoForUI();
            if (info != null) {
                thumbnailLabel.setIcon(info.getThumbnail());
                sizeLabel.setText(info.getSize());
            }

            if (value.isCaptionEdited()) {
                captionLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-important-blue.png"));
                captionLabel.setText(value.getNumTokens() + " / " + (int) Math.max(1, Math.ceil(value.getNumTokens() * 1.0 / 75)) * 75 + " (unsaved)");
            } else {
                captionLabel.setIcon(UIUtils.getIconFromResources("emblems/checkmark.png"));
                captionLabel.setText(value.getNumTokens() + " / " + (int) Math.max(1, Math.ceil(value.getNumTokens() * 1.0 / 75)) * 75);
            }
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
