package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ContentBulkEditExecutionResult")
public class GqlBulkEditExecutionResult {

    private List<String> successfulNodes;
    private List<String> failedNodes;
    private int updatedProperties;
    private List<GqlBulkEditExecutionError> errors;

    @GraphQLField
    public List<String> getSuccessfulNodes() {
        return successfulNodes;
    }

    public void setSuccessfulNodes(List<String> successfulNodes) {
        this.successfulNodes = successfulNodes;
    }

    @GraphQLField
    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(List<String> failedNodes) {
        this.failedNodes = failedNodes;
    }

    @GraphQLField
    public int getUpdatedProperties() {
        return updatedProperties;
    }

    public void setUpdatedProperties(int updatedProperties) {
        this.updatedProperties = updatedProperties;
    }

    @GraphQLField
    public List<GqlBulkEditExecutionError> getErrors() {
        return errors;
    }

    public void setErrors(List<GqlBulkEditExecutionError> errors) {
        this.errors = errors;
    }
}
