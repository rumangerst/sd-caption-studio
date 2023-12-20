package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

public class SDCaptionTemplate extends AbstractJIPipeParameterCollection {
    private String name;
    private HTMLText description;
    private String content;

    @JIPipeDocumentation(name = "Name", description = "The name of the template")
    @JsonGetter("name")
    @JIPipeParameter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Description", description = "A description for the template")
    @JsonGetter("description")
    @JIPipeParameter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    @JIPipeParameter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @JIPipeDocumentation(name = "Content", description = "The content that is inserted for the template")
    @JsonGetter("content")
    @JIPipeParameter("content")
    @StringParameterSettings(monospace = true)
    public String getContent() {
        return content;
    }

    @JsonSetter("content")
    @JIPipeParameter("content")
    public void setContent(String content) {
        this.content = content;
    }

}
