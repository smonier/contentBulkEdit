package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ContentBulkEditSearchFiltersInput")
public class GqlBulkEditSearchFiltersInput {

    private String text;
    private String path;
    private String contentType;
    private String publicationStatus;
    private String publicationFrom;
    private String publicationTo;
    private String creationFrom;
    private String creationTo;
    private String modificationFrom;
    private String modificationTo;
    private String author;

    @GraphQLField
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @GraphQLField
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @GraphQLField
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @GraphQLField
    public String getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(String publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    @GraphQLField
    public String getPublicationFrom() {
        return publicationFrom;
    }

    public void setPublicationFrom(String publicationFrom) {
        this.publicationFrom = publicationFrom;
    }

    @GraphQLField
    public String getPublicationTo() {
        return publicationTo;
    }

    public void setPublicationTo(String publicationTo) {
        this.publicationTo = publicationTo;
    }

    @GraphQLField
    public String getCreationFrom() {
        return creationFrom;
    }

    public void setCreationFrom(String creationFrom) {
        this.creationFrom = creationFrom;
    }

    @GraphQLField
    public String getCreationTo() {
        return creationTo;
    }

    public void setCreationTo(String creationTo) {
        this.creationTo = creationTo;
    }

    @GraphQLField
    public String getModificationFrom() {
        return modificationFrom;
    }

    public void setModificationFrom(String modificationFrom) {
        this.modificationFrom = modificationFrom;
    }

    @GraphQLField
    public String getModificationTo() {
        return modificationTo;
    }

    public void setModificationTo(String modificationTo) {
        this.modificationTo = modificationTo;
    }

    @GraphQLField
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
