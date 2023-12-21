package de.mrnotsoevil.sdcaptionstudio.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

public class SDCaptionTemplate extends AbstractJIPipeParameterCollection {

    private SDCaptionProject project;
    private String name;
    private HTMLText description = new HTMLText();
    private String content;
    private String key;
    private Color color = new Color(0x43A8E0);

    public SDCaptionTemplate() {
    }

    public SDCaptionTemplate(SDCaptionTemplate other) {
        this.project = other.project;
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.content = other.content;
        this.key = other.key;
        this.color = other.color;
    }

    @JIPipeDocumentation(name = "Name", description = "The name of the template")
    @JsonGetter("name")
    @JIPipeParameter(value = "name", uiOrder = -80)
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
        if(description != null) {
            this.description = description;
        }
        else {
            this.description = new HTMLText();
        }
    }

    @JIPipeDocumentation(name = "Content", description = "The content that is inserted for the template")
    @JsonGetter("content")
    @JIPipeParameter(value = "content", uiOrder = -90, important = true)
    @StringParameterSettings(monospace = true, multiline = true)
    public String getContent() {
        return content;
    }

    @JsonSetter("content")
    @JIPipeParameter("content")
    public void setContent(String content) {
        this.content = StringUtils.nullToEmpty(content)
                .replace("\n", " ")
                .replace("\t", " ")
                .replace("\r", " ");
    }

    public SDCaptionProject getProject() {
        return project;
    }

    public void setProject(SDCaptionProject project) {
        this.project = project;
    }

    @JIPipeDocumentation(name = "Key", description = "The key of this template")
    @JIPipeParameter(value = "key", uiOrder = -100, important = true)
    @JsonSetter("key")
    public void setKey(String key) {
        this.key = key;
    }

    @JIPipeParameter("key")
    @JsonGetter("key")
    public String getKey() {
        if(project != null) {
            return project.findTemplateKey(this);
        }
        else {
            return key;
        }
    }

    @JIPipeDocumentation(name = "Color", description = "Color shown in the template list")
    @JIPipeParameter("color")
    @JsonGetter("color")
    public Color getColor() {
        return color;
    }

    @JIPipeParameter("color")
    @JsonSetter("color")
    public void setColor(Color color) {
        this.color = color;
    }

    public void copyMetadataFrom(SDCaptionTemplate other) {
        setName(other.getName());
        setDescription(new HTMLText(other.getDescription()));
    }

    public String getDisplayName() {
        if(!StringUtils.isNullOrEmpty(name)) {
            return name + " [@" + getKey() + "]";
        }
        else {
            return "@" + getKey();
        }
    }
}
