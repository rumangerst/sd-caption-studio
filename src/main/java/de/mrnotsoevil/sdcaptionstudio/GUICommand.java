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

package de.mrnotsoevil.sdcaptionstudio;

import de.mrnotsoevil.sdcaptionstudio.ui.CustomSplashScreen;
import de.mrnotsoevil.sdcaptionstudio.ui.SDCaptionWorkbench;
import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.ijupdater.MissingRegistrationUpdateSiteResolver;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Command that runs the GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>SD Caption Studio")
public class GUICommand implements Command {

    @Parameter
    private PluginService pluginService;

    @Parameter
    private Context context;

    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
//        ij.ui().showUI();
        SwingUtilities.invokeLater(() -> ij.command().run(GUICommand.class, true));
    }

    @Override
    public void run() {
        // Update look & feel
        UIUtils.loadLookAndFeelFromSettings();
        if (!JIPipe.isInstantiated()) {
            SwingUtilities.invokeLater(() -> CustomSplashScreen.getInstance().showSplash(context));
        }

        // Check java dependencies
        if (!checkJavaDependencies()) {
            SwingUtilities.invokeLater(() -> CustomSplashScreen.getInstance().hideSplash());
            if (JOptionPane.showConfirmDialog(null,
                    "JIPipe has detected that you might miss some essential files. Please " +
                            "ensure that you installed following dependencies:\nGuava\nFlexMark\nJackson\nJGraphT\nOpenHTMLToPDF\nMSLinks\nApache Commons\nFontBox\nPDFBox\nAutoLink \n\n" +
                            "Do you want to continue anyway?", "Missing dependencies", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
            SwingUtilities.invokeLater(() -> CustomSplashScreen.getInstance().showSplash(context));
        }

        // Run registration
        ExtensionSettings extensionSettings = ExtensionSettings.getInstanceFromRaw();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        try {
            if (JIPipe.getInstance() == null) {
                JIPipe jiPipe = JIPipe.createInstance(context);
                CustomSplashScreen.getInstance().setJIPipe(JIPipe.getInstance());
                jiPipe.initialize(extensionSettings, issues);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!extensionSettings.isSilent())
                UIUtils.openErrorDialog(new JIPipeDummyWorkbench(), null, e);
            return;
        }

        // Run application
        SwingUtilities.invokeLater(() -> {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);

            CustomSplashScreen.getInstance().hideSplash();

            SDCaptionWorkbench window = new SDCaptionWorkbench(getContext());
            window.setVisible(true);
            window.setExtendedState(window.getExtendedState() + Frame.MAXIMIZED_BOTH);
            window.toFront();
        });
    }

    private boolean checkJavaDependencies() {
        List<String> classesToTest = Arrays.asList("com.google.common.base.Charsets",
                "com.google.common.io.Resources",
                "com.google.common.collect.BiMap",
                "com.google.common.collect.HashBiMap",
                "com.google.common.collect.ImmutableBiMap",
                "com.google.common.collect.ImmutableList",
                "com.google.common.collect.ImmutableSet",
                "com.google.common.eventbus.EventBus",
                "com.google.common.eventbus.Subscribe",
                "com.vladsch.flexmark.ext.autolink.AutolinkExtension",
                "com.vladsch.flexmark.ext.tables.TablesExtension",
                "com.vladsch.flexmark.ext.toc.TocExtension",
                "com.vladsch.flexmark.html.HtmlRenderer",
                "com.vladsch.flexmark.parser.Parser",
                "com.vladsch.flexmark.util.ast.Node",
                "com.vladsch.flexmark.util.data.MutableDataHolder",
                "com.vladsch.flexmark.util.data.MutableDataSet",
                "com.fasterxml.jackson.core.JsonGenerator",
                "com.fasterxml.jackson.core.JsonParser",
                "com.fasterxml.jackson.core.JsonProcessingException",
                "com.fasterxml.jackson.databind.DeserializationContext",
                "com.fasterxml.jackson.databind.JsonDeserializer",
                "com.fasterxml.jackson.databind.JsonNode",
                "com.fasterxml.jackson.databind.JsonSerializer",
                "com.fasterxml.jackson.databind.SerializerProvider",
                "com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                "com.fasterxml.jackson.databind.annotation.JsonSerialize",
                "org.jgrapht.Graph",
                "org.jgrapht.alg.cycle.CycleDetector",
                "org.jgrapht.graph.DefaultDirectedGraph",
                "org.jgrapht.traverse.GraphIterator",
                "org.jgrapht.traverse.TopologicalOrderIterator",
                "com.openhtmltopdf.pdfboxout.PdfBoxImage",
                "com.openhtmltopdf.bidi.support.ICUBreakers",
                "mslinks.ShellLink",
                "org.apache.commons.io.FileUtils",
                "org.apache.fontbox.FontBoxFont",
                "org.apache.pdfbox.rendering.PDFRenderer",
                "org.nibor.autolink.Autolink");
        boolean success = true;
        for (String klass : classesToTest) {
            try {
                Class.forName(klass);
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                e.printStackTrace();
                System.err.println("Failed to find class: " + klass + ". Are all dependencies installed?");
                success = false;
            }
        }
        return success;
    }

    private void resolveMissingImageJDependencies(JIPipeRegistryIssues issues) {
        if (issues.getMissingImageJSites().isEmpty())
            return;
        MissingRegistrationUpdateSiteResolver resolver = new MissingRegistrationUpdateSiteResolver(getContext(), issues);
        resolver.revalidate();
        resolver.repaint();
        resolver.setLocationRelativeTo(null);
        resolver.setVisible(true);

    }

    /**
     * @return The context
     */
    public Context getContext() {
        return context;
    }
}
