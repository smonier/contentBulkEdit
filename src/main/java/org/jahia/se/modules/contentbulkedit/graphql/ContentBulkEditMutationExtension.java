package org.jahia.se.modules.contentbulkedit.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public final class ContentBulkEditMutationExtension {

    private ContentBulkEditMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("contentBulkEdit")
    @GraphQLDescription("Content bulk edit operations")
    public static ContentBulkEditOperations contentBulkEdit() {
        return new ContentBulkEditOperations();
    }
}
