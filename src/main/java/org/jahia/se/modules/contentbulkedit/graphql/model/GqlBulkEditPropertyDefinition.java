package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ContentBulkEditPropertyDefinition")
@GraphQLDescription("Editable property definition with full type metadata for bulk edit")
public class GqlBulkEditPropertyDefinition {

    private String name;
    private String label;
    private String requiredType;
    private String selectorType;
    private String declaringNodeType;
    private String declaringNodeTypeLabel;
    private List<GqlBulkEditSelectorOption> selectorOptions;
    private List<String> constraints;
    private List<String> defaultValues;
    private boolean internationalized;
    private boolean multiple;
    private boolean mandatory;

    @GraphQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLField
    @GraphQLDescription("Editor-facing label resolved from the module resource bundle")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @GraphQLField
    @GraphQLDescription("JCR required type name, e.g. String, WeakReference, Date, Boolean, Long")
    public String getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(String requiredType) {
        this.requiredType = requiredType;
    }

    @GraphQLField
    @GraphQLDescription("CND selector type name, e.g. Choicelist, Picker, RichText, DatePicker")
    public String getSelectorType() {
        return selectorType;
    }

    public void setSelectorType(String selectorType) {
        this.selectorType = selectorType;
    }

    @GraphQLField
    @GraphQLDescription("Node type that declares this property, e.g. the primary type or a mixin")
    public String getDeclaringNodeType() {
        return declaringNodeType;
    }

    public void setDeclaringNodeType(String declaringNodeType) {
        this.declaringNodeType = declaringNodeType;
    }

    @GraphQLField
    @GraphQLDescription("Editor-facing label of the declaring node type")
    public String getDeclaringNodeTypeLabel() {
        return declaringNodeTypeLabel;
    }

    public void setDeclaringNodeTypeLabel(String declaringNodeTypeLabel) {
        this.declaringNodeTypeLabel = declaringNodeTypeLabel;
    }

    @GraphQLField
    @GraphQLDescription("Selector options declared in the CND, e.g. picker type='image'")
    public List<GqlBulkEditSelectorOption> getSelectorOptions() {
        return selectorOptions;
    }

    public void setSelectorOptions(List<GqlBulkEditSelectorOption> selectorOptions) {
        this.selectorOptions = selectorOptions;
    }

    @GraphQLField
    @GraphQLDescription("Static value constraints, e.g. choicelist values")
    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    @GraphQLField
    public List<String> getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(List<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    @GraphQLField
    public boolean isInternationalized() {
        return internationalized;
    }

    public void setInternationalized(boolean internationalized) {
        this.internationalized = internationalized;
    }

    @GraphQLField
    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @GraphQLField
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
