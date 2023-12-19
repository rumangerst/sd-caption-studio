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

package de.mrnotsoevil.sdcaptionstudio.ui;

import de.mrnotsoevil.sdcaptionstudio.SDCaptionStudio;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.*;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ImageFrame;
import org.hkijena.jipipe.ui.components.RoundedButtonUI;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.jar.Attributes;

/**
 * UI that shows some introduction
 */
public class CustomWelcomePanel extends JIPipeWorkbenchPanel {
    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public CustomWelcomePanel(JIPipeWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);

        initializeRecentProjects(splitPane);
        initializeHero(splitPane);

        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeHero(AutoResizeSplitPane splitPane) {
        BufferedImage backgroundImage;
        try {
            if (UIUtils.DARK_THEME) {
                backgroundImage = ImageIO.read(SDCaptionStudio.RESOURCES.getResourceAsStream("welcome-hero-dark.png"));
            } else {
                backgroundImage = ImageIO.read(SDCaptionStudio.RESOURCES.getResourceAsStream("welcome-hero.png"));
            }
        } catch (Throwable e) {
            backgroundImage = null;
        }
        JPanel heroPanel = new ImageFrame(backgroundImage, false, SizeFitMode.Cover, true);
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));

        heroPanel.add(Box.createVerticalGlue());
        initializeHeroLogo(heroPanel);
        initializeHeroActions(heroPanel);
        heroPanel.add(Box.createVerticalStrut(16));
        initializeHeroSecondaryActions(heroPanel);
        heroPanel.add(Box.createVerticalGlue());
        initializeHeroBottomPanel(heroPanel);

        splitPane.setRightComponent(heroPanel);
    }

    private void initializeHeroSecondaryActions(JPanel heroPanel) {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        actionPanel.add(Box.createHorizontalGlue());

        actionPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        heroPanel.add(actionPanel);
    }

    private void initializeHeroActions(JPanel heroPanel) {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        actionPanel.add(Box.createHorizontalGlue());

        Color colorSuccess = new Color(0x5CB85C);
        Color colorHover = new Color(0x4f9f4f);

        JButton startNowButton = new JButton("Open directory");
        startNowButton.setBackground(colorSuccess);
        startNowButton.setForeground(Color.WHITE);
        startNowButton.setUI(new RoundedButtonUI(8, colorHover, colorHover));
        startNowButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 28));
        startNowButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        startNowButton.addActionListener(e -> doActionOpenProject());
        actionPanel.add(startNowButton);

        actionPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 120));

        actionPanel.add(Box.createHorizontalGlue());
        heroPanel.add(actionPanel);
    }

    private void doActionOpenProject() {
        ((SDCaptionWorkbench)getWorkbench()).openDirectory();
    }

    private void initializeHeroBottomPanel(JPanel heroPanel) {

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));

        initializeHeroLinksPanel(bottomPanel);
        initializeHeroTechnicalInfoPanel(bottomPanel);

        heroPanel.add(bottomPanel);
    }

    private void initializeHeroLinksPanel(JPanel bottomPanel) {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);

        toolBar.add(Box.createHorizontalStrut(8));

        JButton openSourceCodeButton = new JButton("Source code", UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
        openSourceCodeButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        openSourceCodeButton.setToolTipText("https://github.com/rumangerst/sd-caption-studio");
        openSourceCodeButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/rumangerst/sd-caption-studio"));
        openSourceCodeButton.setOpaque(false);
        openSourceCodeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openSourceCodeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton reportIssueButton = new JButton("Report issue", UIUtils.getIconFromResources("actions/bug.png"));
        reportIssueButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        reportIssueButton.setToolTipText("https://github.com/rumangerst/sd-caption-studio/issues");
        reportIssueButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/rumangerst/sd-caption-studio/issues"));
        reportIssueButton.setOpaque(false);
        reportIssueButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(reportIssueButton);
        toolBar.add(Box.createHorizontalStrut(4));

        bottomPanel.add(toolBar, BorderLayout.WEST);

    }

    private void initializeHeroTechnicalInfoPanel(JPanel bottomPanel) {

        FormPanel technicalInfo = new FormPanel(null, FormPanel.TRANSPARENT_BACKGROUND);
        technicalInfo.addVerticalGlue();

        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(getClass().getPackage().getImplementationVersion(), "Development")), new JLabel("Version"), null);
        Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
        if (manifestAttributes != null) {
            String implementationDateString = manifestAttributes.getValue("Implementation-Date");
            technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(implementationDateString, "N/A")), new JLabel("Build time"), null);
        }
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(IJ.getVersion(), "N/A")), new JLabel("ImageJ"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(System.getProperty("java.version"), "N/A")), new JLabel("Java"), null);

        technicalInfo.setMaximumSize(new Dimension(300, 200));

        bottomPanel.add(technicalInfo, BorderLayout.EAST);
    }

    private void initializeHeroLogo(JPanel heroPanel) {
//        ImageFrame logoPanel = new ImageFrame(UIUtils.getLogo(), false, SizeFitMode.Fit, true);
//        logoPanel.setScaleFactor(0.7);
//        logoPanel.setOpaque(false);
//        heroPanel.add(logoPanel);
    }

    private void initializeRecentProjects(AutoResizeSplitPane splitPane) {
        DocumentTabPane tabPane = new DocumentTabPane(true);

        // Recent projects list
        initRecentProjects(tabPane);

        splitPane.setLeftComponent(tabPane);
    }

    private void initRecentProjects(DocumentTabPane tabPane) {
        tabPane.addTab("Recent projects",
                UIUtils.getIconFromResources("actions/view-calendar-time-spent.png"),
                new CustomRecentProjectsListPanel(getWorkbench()),
                DocumentTabPane.CloseMode.withoutCloseButton);
    }
}
