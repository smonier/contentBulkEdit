package org.jahia.se.modules.contentbulkedit.graphql.model;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ContentBulkEditSearchResult")
public class GqlBulkEditSearchResult {

    private List<GqlBulkEditNode> nodes;
    private int totalCount;
    private boolean truncated;

    @GraphQLField
    public List<GqlBulkEditNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GqlBulkEditNode> nodes) {
        this.nodes = nodes;
    }

    @GraphQLField
    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @GraphQLField
    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }
}
