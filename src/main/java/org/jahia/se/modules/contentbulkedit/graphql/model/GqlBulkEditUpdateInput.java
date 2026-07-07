package org.jahia.se.modules.contentbulkedit.graphql.model;

/**
 * Internal DTO pairing a property name with its raw (string) bulk value.
 * Type coercion and internationalization are resolved server-side from the
 * applicable property definition, never from the client.
 */
public class GqlBulkEditUpdateInput {

    private String name;
    private String value;
    private String mode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
