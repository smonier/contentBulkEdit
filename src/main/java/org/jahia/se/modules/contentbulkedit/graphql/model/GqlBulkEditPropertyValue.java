package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ContentBulkEditPropertyValue")
public class GqlBulkEditPropertyValue {

    private String name;
    private String value;
    private List<String> values;
    private boolean multiple;

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

    @GraphQLField
    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @GraphQLField
    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }
}
