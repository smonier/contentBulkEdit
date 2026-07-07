package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ContentBulkEditSelectorOption")
@GraphQLDescription("A selector option declared on a property definition, e.g. picker type")
public class GqlBulkEditSelectorOption {

    private String name;
    private String value;

    @GraphQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLField
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
