package de.mrnotsoevil.sdcaptionstudio.ui.imagelist;

import com.google.common.collect.Sets;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionProject;
import de.mrnotsoevil.sdcaptionstudio.api.SDCaptionedImage;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDCaptionedImageRenamingUtil extends AbstractJIPipeParameterCollection {

    public static final SDCaptionedImageRenamingUtil INSTANCE = new SDCaptionedImageRenamingUtil();
    private DefaultExpressionParameter renamingFunction = new DefaultExpressionParameter("old_name + \"_\" + index");

    public SDCaptionedImageRenamingUtil() {
    }

    public SDCaptionedImageRenamingUtil(SDCaptionedImageRenamingUtil other) {
        this.renamingFunction = new DefaultExpressionParameter(other.renamingFunction);
    }

    @JIPipeDocumentation(name = "Renaming function", description = "The function is applied per image and should return the new name")
    @JIPipeParameter("renaming-function")
    @ExpressionParameterSettings(hint = "per image")
    @ExpressionParameterSettingsVariable(name = "Old name", key = "old_name", description = "The old name of the image (excluding extensions)")
    @ExpressionParameterSettingsVariable(name = "Index", key = "index", description = "The index in the sorted list of images")
    @ExpressionParameterSettingsVariable(name = "Local index", key = "local_index", description = "The index in the selection (if applied to a set of images). Otherwise the same as 'index'")
    public DefaultExpressionParameter getRenamingFunction() {
        return renamingFunction;
    }

    @JIPipeParameter("renaming-function")
    public void setRenamingFunction(DefaultExpressionParameter renamingFunction) {
        this.renamingFunction = renamingFunction;
    }

    public void apply(SDCaptionProject project, List<SDCaptionedImage> imagesToRename) {
        Map<String, String> renamingMap = new HashMap<>();
        List<SDCaptionedImage> sortedImages = project.getSortedImages();
        for (int i = 0; i < sortedImages.size(); i++) {
            SDCaptionedImage image = sortedImages.get(i);
            if(imagesToRename == null || imagesToRename.contains(image)) {
                ExpressionVariables variables = new ExpressionVariables();
                variables.set("index", i);
                variables.set("local_index", imagesToRename != null ? imagesToRename.indexOf(image) : i);
                variables.set("old_name", image.getName());
                String newName = renamingFunction.evaluateToString(variables).trim();
                if(StringUtils.isNullOrEmpty(newName)) {
                    throw new IllegalArgumentException("Invalid new name for '" + image.getName() + "'");
                }
                renamingMap.put(image.getName(), newName);
            }
        }
        if(Sets.newHashSet(renamingMap.values()).size() != renamingMap.keySet().size()) {
            throw new IllegalArgumentException("Non-unique names detected!");
        }
        for (Map.Entry<String, String> entry : renamingMap.entrySet()) {
            if(!entry.getKey().equals(entry.getValue())) {
                if(project.getImages().containsKey(entry.getValue())) {
                    throw new IllegalArgumentException("Name already exists: " + entry.getValue());
                }
            }
        }
        for (Map.Entry<String, String> entry : renamingMap.entrySet()) {
            if (!entry.getKey().equals(entry.getValue())) {
                project.renameImage(entry.getKey(), entry.getValue());
            }
        }
    }
}
