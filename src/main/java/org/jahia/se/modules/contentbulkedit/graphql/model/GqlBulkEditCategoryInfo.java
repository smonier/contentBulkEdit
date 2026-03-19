package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ContentBulkEditCategoryInfo")
public class GqlBulkEditCategoryInfo {

    private String identifier;
    private String parentIdentifier;
    private String path;
    private String name;
    private String label;
    private boolean hasChildren;

    @GraphQLField
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @GraphQLField
    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    @GraphQLField
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @GraphQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLField
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @GraphQLField
    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
}
