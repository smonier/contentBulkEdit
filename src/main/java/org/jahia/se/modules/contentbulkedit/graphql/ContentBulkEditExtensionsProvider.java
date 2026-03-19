package org.jahia.se.modules.contentbulkedit.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class ContentBulkEditExtensionsProvider implements DXGraphQLExtensionsProvider {
}
