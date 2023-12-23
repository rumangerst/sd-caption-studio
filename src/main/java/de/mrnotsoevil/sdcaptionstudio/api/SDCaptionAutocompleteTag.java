package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.mrnotsoevil.sdcaptionstudio.api.events.SDCaptionProjectTagsChangedEvent;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.StringUtils;

public class SDCaptionAutocompleteTag {
    private SDCaptionProject project;
    private String key;
    private String replacement;
    private String source;

    @JIPipeDocumentation(name = "Key", description = "The text to be autocompleted")
    @JIPipeParameter(value = "key", important = true, uiOrder = -100)
    @JsonGetter("key")
    public String getKey() {
        return StringUtils.nullToEmpty(key);
    }

    @JsonSetter("key")
    @JIPipeParameter("key")
    public void setKey(String key) {
        this.key = key;
        if(project != null) {
            project.getProjectTagsChangedEventEmitter().emit(new SDCaptionProjectTagsChangedEvent(project));
        }
    }

    @JIPipeDocumentation(name = "Replacement (optional)", description = "Text that will replace the key on auto-completion. " +
            "If left empty, the key will be auto-completed as-is")
    @JsonGetter("replacement")
    @JIPipeParameter(value = "replacement", uiOrder = -90)
    public String getReplacement() {
        return replacement;
    }

    @JsonSetter("replacement")
    @JIPipeParameter("replacement")
    public void setReplacement(String replacement) {
        this.replacement = replacement;
        if(project != null) {
            project.getProjectTagsChangedEventEmitter().emit(new SDCaptionProjectTagsChangedEvent(project));
        }
    }

    @JIPipeDocumentation(name = "Source (optional)", description = "Source of this tag")
    @JsonGetter("source")
    @JIPipeParameter("source")
    public String getSource() {
        return source;
    }

    @JsonSetter("source")
    @JIPipeParameter("source")
    public void setSource(String source) {
        this.source = source;
    }

    public SDCaptionProject getProject() {
        return project;
    }

    public void setProject(SDCaptionProject project) {
        this.project = project;
    }
}
