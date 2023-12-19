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
    private JLabel sizeLabel;
    private final Map<String, ImageInfo> imageInfos = new HashMap<>();
    private final StampedLock imageInfosLock = new StampedLock();

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

        if(value != null) {
            nameLabel.setText(value.getName());
            captionLabel.setText(value.getNumTokens() + " / " + (int)Math.max(1, Math.ceil(value.getNumTokens() * 1.0 / 75)) * 75);

            // Try loading the thumbnail
            long stamp = imageInfosLock.readLock();
            try {
                ImageInfo info = imageInfos.getOrDefault(value.getName(), null);
                if(info != null) {
                    thumbnailLabel.setIcon(info.thumbnail);
                    sizeLabel.setText(info.size);
                }
                else {
                    // Load from image
                    stamp = imageInfosLock.tryConvertToWriteLock(stamp);
                    info = new ImageInfo();
                    info.size = "Loading ...";
                    info.thumbnail = loadingIcon;
                    imageInfos.put(value.getName(), info);

                    // Schedule
                    ThumbnailLoader loader = new ThumbnailLoader(value, list);
                    loader.execute();
                }
            }
            finally {
                imageInfosLock.unlock(stamp);
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

    public static class ImageInfo {
        private ImageIcon thumbnail;
        private String size;

        public ImageIcon getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(ImageIcon thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }
    }

    public class ThumbnailLoader extends SwingWorker<ImageInfo, Object> {

        private final SDCaptionedImage captionedImage;
        private final JList<? extends SDCaptionedImage> list;

        public ThumbnailLoader(SDCaptionedImage captionedImage, JList<? extends SDCaptionedImage> list) {
            this.captionedImage = captionedImage;
            this.list = list;
        }

        @Override
        protected ImageInfo doInBackground() throws Exception {
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
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.thumbnail = new ImageIcon(resized.createImage());
            imageInfo.size = image.getWidth() + "x" + image.getHeight();
            return imageInfo;
        }

        @Override
        protected void done() {
            long stamp = imageInfosLock.writeLock();
            try {
                ImageInfo imageInfo = get();
                imageInfos.put(captionedImage.getName(), imageInfo);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.thumbnail = UIUtils.getIconFromResources("emblems/vcs-conflicting.png");
                imageInfo.size = "<Unable to load>";
                imageInfos.put(captionedImage.getName(),imageInfo );
            }
            finally {
                imageInfosLock.unlock(stamp);
                list.repaint();
            }
        }
    }
}
