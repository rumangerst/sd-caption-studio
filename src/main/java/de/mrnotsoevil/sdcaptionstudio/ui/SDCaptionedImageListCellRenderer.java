package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.StampedLock;

public class SDCaptionedImageListCellRenderer extends JPanel implements ListCellRenderer<SDCaptionedImage> {
    private final SDCaptionProject project;
    private final ImageIcon loadingIcon = UIUtils.getIcon16FromResources("actions/hourglass-half.png");
    private int thumbnailSize = 64;
    private JLabel thumbnailLabel;
    private JLabel nameLabel;
    private JLabel captionLabel;
    private final Map<String, ImageIcon> thumbnails = new HashMap<>();
    private final StampedLock thumbnailsLock = new StampedLock();

    public SDCaptionedImageListCellRenderer(SDCaptionProject project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Insets defaultInsets = new Insets(4,4,4,4);
        setLayout(new GridBagLayout());
        thumbnailLabel = new JLabel(loadingIcon, JLabel.CENTER);
        thumbnailLabel.setPreferredSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setMaximumSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setMinimumSize(new Dimension(thumbnailSize, thumbnailSize));
        thumbnailLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        nameLabel = new JLabel();
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        captionLabel = new JLabel();
        captionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));

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

        if(value != null) {
            nameLabel.setText(value.getName());
            captionLabel.setText(value.getNumTokens() + " / " + (int)Math.max(1, Math.ceil(value.getNumTokens() * 1.0 / 75)) * 75);

            // Try loading the thumbnail
            long stamp = thumbnailsLock.readLock();
            try {
                ImageIcon icon = thumbnails.getOrDefault(value.getName(), null);
                if(icon != null) {
                    thumbnailLabel.setIcon(icon);
                }
                else {
                    // Load from image
                    stamp = thumbnailsLock.tryConvertToWriteLock(stamp);
                    thumbnails.put(value.getName(), loadingIcon);

                    // Schedule
                    ThumbnailLoader loader = new ThumbnailLoader(value, list);
                    loader.execute();
                }
            }
            finally {
                thumbnailsLock.unlock(stamp);
            }
        }

        if (isSelected) {
            if(UIUtils.DARK_THEME) {
                setBackground(new Color(0x2A537A));
            }
            else {
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

    public class ThumbnailLoader extends SwingWorker<ImageIcon, Object> {

        private final SDCaptionedImage captionedImage;
        private final JList<? extends SDCaptionedImage> list;

        public ThumbnailLoader(SDCaptionedImage captionedImage, JList<? extends SDCaptionedImage> list) {
            this.captionedImage = captionedImage;
            this.list = list;
        }

        @Override
        protected ImageIcon doInBackground() throws Exception {
            if(!Files.isRegularFile(captionedImage.getImagePath())) {
                throw new FileNotFoundException(captionedImage.getImagePath() + " does not exist!");
            }
            ImagePlus image = IJ.openImage(captionedImage.getImagePath().toString());
            double factorX = 1.0 * thumbnailSize / image.getWidth();
            double factorY = 1.0 * thumbnailSize / image.getHeight();
            double factor = Math.max(factorX, factorY);
            boolean smooth = factor < 0;
            image.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
            ImageProcessor resized = image.getProcessor().resize((int) (factorX * image.getWidth()),
                    (int) (factorY * image.getHeight()),
                    smooth);
            return new ImageIcon(resized.createImage());
        }

        @Override
        protected void done() {
            long stamp = thumbnailsLock.writeLock();
            try {
                ImageIcon imageIcon = get();
                thumbnails.put(captionedImage.getName(), imageIcon);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                thumbnails.put(captionedImage.getName(), UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
            finally {
                thumbnailsLock.unlock(stamp);
                list.repaint();
            }
        }
    }
}
