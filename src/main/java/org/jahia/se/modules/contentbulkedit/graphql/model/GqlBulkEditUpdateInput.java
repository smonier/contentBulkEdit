package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ContentBulkEditUpdateInput")
public class GqlBulkEditUpdateInput {

    private String name;
    private String value;
    private boolean i18n;

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
    public boolean isI18n() {
        return i18n;
    }

    public void setI18n(boolean i18n) {
        this.i18n = i18n;
    }
}
