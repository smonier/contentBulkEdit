package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ContentBulkEditNode")
public class GqlBulkEditNode {

    private String uuid;
    private String path;
    private String name;
    private String displayName;
    private String nodeType;
    private String nodeTypeLabel;
    private String publicationStatus;
    private String publicationDate;
    private String created;
    private String lastModified;
    private String author;
    private List<GqlBulkEditPropertyValue> propertyValues;
    private List<String> tags;
    private List<String> categories;

    @GraphQLField
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @GraphQLField
    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    @GraphQLField
    public String getNodeTypeLabel() {
        return nodeTypeLabel;
    }

    public void setNodeTypeLabel(String nodeTypeLabel) {
        this.nodeTypeLabel = nodeTypeLabel;
    }

    @GraphQLField
    public String getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(String publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    @GraphQLField
    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    @GraphQLField
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    @GraphQLField
    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @GraphQLField
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @GraphQLField
    public List<GqlBulkEditPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(List<GqlBulkEditPropertyValue> propertyValues) {
        this.propertyValues = propertyValues;
    }

    @GraphQLField
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @GraphQLField
    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
