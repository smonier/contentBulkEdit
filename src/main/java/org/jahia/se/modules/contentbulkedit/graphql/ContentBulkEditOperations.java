package org.jahia.se.modules.contentbulkedit.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditCategoryInfo;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditExecutionError;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditExecutionResult;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditNode;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditPropertyValue;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditSearchFiltersInput;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditSearchResult;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditUpdateInput;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@GraphQLName("ContentBulkEditOperations")
@GraphQLDescription("Content bulk edit operations")
public class ContentBulkEditOperations {

    private static final Logger logger = LoggerFactory.getLogger(ContentBulkEditOperations.class);
    private static final Pattern SITE_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern NODE_TYPE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9:_-]*$");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9:_-]*$");
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[A-Za-z]{2,8}([_-][A-Za-z0-9]{2,8})*$");
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_OUTPUT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_SCAN = 5000;
    private static final String CATEGORY_ROOT = "/sites/systemsite/categories";

    private JCRTemplate jcrTemplate;

    @Inject
    @GraphQLOsgiService
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @GraphQLField
    @GraphQLDescription("Search content to prepare a bulk edit")
    public GqlBulkEditSearchResult searchContent(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("text") String text,
            @GraphQLName("path") String path,
            @GraphQLName("contentType") String contentType,
            @GraphQLName("publicationStatus") String publicationStatus,
            @GraphQLName("publicationFrom") String publicationFrom,
            @GraphQLName("publicationTo") String publicationTo,
            @GraphQLName("creationFrom") String creationFrom,
            @GraphQLName("creationTo") String creationTo,
            @GraphQLName("modificationFrom") String modificationFrom,
            @GraphQLName("modificationTo") String modificationTo,
            @GraphQLName("author") String author,
            @GraphQLName("properties") List<String> properties,
            @GraphQLName("limit") Integer limit
    ) throws RepositoryException {
        ensureJcrTemplate();

        final String validatedSiteKey = validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);
        final GqlBulkEditSearchFiltersInput safeFilters = sanitizeFilters(buildFilters(
                text,
                path,
                contentType,
                publicationStatus,
                publicationFrom,
                publicationTo,
                creationFrom,
                creationTo,
                modificationFrom,
                modificationTo,
                author
        ));
        final List<String> safeProperties = sanitizePropertyNames(properties);
        final int effectiveLimit = resolveLimit(limit);

        return jcrTemplate.doExecuteWithSystemSessionAsUser(null, "default", null, session -> {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String searchStatement = buildSearchQuery(validatedSiteKey, safeFilters);
            logger.info("contentBulkEdit searchContent siteKey={} language={} filters={} query={}",
                    validatedSiteKey, normalizedLanguage, summarizeFilters(safeFilters), searchStatement);
            QueryResult result = queryManager.createQuery(searchStatement, Query.JCR_SQL2).execute();

            Set<String> processed = new LinkedHashSet<>();
            List<GqlBulkEditNode> nodes = new ArrayList<>();
            int scanned = 0;
            boolean truncated = false;

            NodeIterator iterator = result.getNodes();
            while (iterator.hasNext()) {
                Node rawNode = iterator.nextNode();
                scanned++;

                if (scanned > MAX_SCAN || nodes.size() >= effectiveLimit) {
                    truncated = true;
                    break;
                }

                JCRNodeWrapper wrapper = toNodeWrapper(session, rawNode);
                JCRNodeWrapper searchableNode = resolveSearchableNode(wrapper);
                if (searchableNode == null) {
                    continue;
                }

                if (!processed.add(searchableNode.getIdentifier())) {
                    continue;
                }

                GqlBulkEditNode gqlNode = buildSearchNode(searchableNode, normalizedLanguage, safeFilters, safeProperties);
                if (gqlNode != null) {
                    nodes.add(gqlNode);
                }
            }

            GqlBulkEditSearchResult searchResult = new GqlBulkEditSearchResult();
            searchResult.setNodes(nodes);
            searchResult.setTotalCount(nodes.size());
            searchResult.setTruncated(truncated || iterator.hasNext());
            return searchResult;
        });
    }

    @GraphQLField
    @GraphQLName("getCategories")
    @GraphQLDescription("Retrieve categories as a flat tree")
    public List<GqlBulkEditCategoryInfo> getCategories(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language
    ) throws RepositoryException {
        ensureJcrTemplate();
        validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);

        return jcrTemplate.doExecuteWithSystemSessionAsUser(null, "default", null, session -> {
            if (!session.nodeExists(CATEGORY_ROOT)) {
                return Collections.emptyList();
            }

            JCRNodeWrapper root = session.getNode(CATEGORY_ROOT);
            List<GqlBulkEditCategoryInfo> categories = new ArrayList<>();
            collectCategories(root, normalizedLanguage, categories, null);
            return categories;
        });
    }

    @GraphQLField
    @GraphQLDescription("Execute a bulk edit on selected nodes")
    public GqlBulkEditExecutionResult bulkEditContent(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("nodeUuids") @GraphQLNonNull List<String> nodeUuids,
            @GraphQLName("propertyUpdates") List<GqlBulkEditUpdateInput> propertyUpdates,
            @GraphQLName("propertyNames") List<String> propertyNames,
            @GraphQLName("propertyValues") List<String> propertyValues,
            @GraphQLName("propertyInternationalized") List<Boolean> propertyInternationalized,
            @GraphQLName("tagValues") List<String> tagValues,
            @GraphQLName("categoryIdentifiers") List<String> categoryIdentifiers
    ) throws RepositoryException {
        ensureJcrTemplate();

        final String validatedSiteKey = validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);
        final List<GqlBulkEditUpdateInput> safeUpdates = sanitizeUpdates(resolvePropertyUpdates(
                propertyUpdates,
                propertyNames,
                propertyValues,
                propertyInternationalized
        ));
        final List<String> safeTags = sanitizeStringValues(tagValues);
        final List<String> safeCategoryIdentifiers = sanitizeStringValues(categoryIdentifiers);

        return jcrTemplate.doExecuteWithSystemSessionAsUser(null, "default", null, session -> {
            GqlBulkEditExecutionResult executionResult = new GqlBulkEditExecutionResult();
            executionResult.setSuccessfulNodes(new ArrayList<>());
            executionResult.setFailedNodes(new ArrayList<>());
            executionResult.setErrors(new ArrayList<>());
            executionResult.setUpdatedProperties(0);

            if ((safeUpdates == null || safeUpdates.isEmpty()) &&
                    (safeTags == null || safeTags.isEmpty()) &&
                    (safeCategoryIdentifiers == null || safeCategoryIdentifiers.isEmpty())) {
                return executionResult;
            }

            for (String nodeUuid : nodeUuids) {
                if (StringUtils.isBlank(nodeUuid)) {
                    continue;
                }

                try {
                    JCRNodeWrapper node = session.getNodeByIdentifier(nodeUuid);
                    if (!node.getPath().startsWith("/sites/" + validatedSiteKey + "/") &&
                            !node.getPath().equals("/sites/" + validatedSiteKey)) {
                        throw new RepositoryException("Node is outside of the requested site");
                    }

                    JCRNodeWrapper translationNode = getTranslationNode(node, normalizedLanguage);
                    int updatedCount = 0;

                    if (safeUpdates != null) {
                        for (GqlBulkEditUpdateInput update : safeUpdates) {
                            updatedCount += updateStringProperty(node, normalizedLanguage, update.getName(), update.getValue(), update.isI18n());
                        }
                    }

                    if (safeTags != null && !safeTags.isEmpty()) {
                        ensureMixin(node, "jmix:tagged");
                        node.setProperty("j:tagList", safeTags.toArray(new String[0]));
                        updatedCount++;
                    }

                    if (safeCategoryIdentifiers != null && !safeCategoryIdentifiers.isEmpty()) {
                        ensureMixin(node, "jmix:categorized");
                        node.setProperty("j:defaultCategory", safeCategoryIdentifiers.toArray(new String[0]));
                        updatedCount++;
                    }

                    executionResult.getSuccessfulNodes().add(nodeUuid);
                    executionResult.setUpdatedProperties(executionResult.getUpdatedProperties() + updatedCount);
                } catch (Exception e) {
                    logger.error("Bulk edit failed on node {}", nodeUuid, e);
                    executionResult.getFailedNodes().add(nodeUuid);
                    GqlBulkEditExecutionError error = new GqlBulkEditExecutionError();
                    error.setNodeUuid(nodeUuid);
                    error.setMessage(e.getMessage());
                    try {
                        error.setNodePath(session.getNodeByIdentifier(nodeUuid).getPath());
                    } catch (Exception ignored) {
                        error.setNodePath(null);
                    }
                    executionResult.getErrors().add(error);
                }
            }

            session.save();
            return executionResult;
        });
    }

    private void ensureJcrTemplate() {
        if (jcrTemplate == null) {
            throw new IllegalStateException("JCRTemplate service is unavailable");
        }
    }

    private String validateSiteKey(String siteKey) {
        String trimmed = StringUtils.trimToNull(siteKey);
        if (trimmed == null || !SITE_KEY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid site key");
        }

        return trimmed;
    }

    private String normalizeLanguage(String language) {
        String normalized = StringUtils.trimToEmpty(language).replace('-', '_');
        if (StringUtils.isBlank(normalized)) {
            return "en";
        }

        if (!LANGUAGE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid language");
        }

        return normalized;
    }

    private GqlBulkEditSearchFiltersInput sanitizeFilters(GqlBulkEditSearchFiltersInput filters) {
        if (filters == null) {
            return null;
        }

        GqlBulkEditSearchFiltersInput safe = new GqlBulkEditSearchFiltersInput();
        safe.setText(StringUtils.trimToNull(filters.getText()));
        safe.setPath(StringUtils.trimToNull(filters.getPath()));
        safe.setPublicationStatus(StringUtils.trimToNull(filters.getPublicationStatus()));
        safe.setPublicationFrom(StringUtils.trimToNull(filters.getPublicationFrom()));
        safe.setPublicationTo(StringUtils.trimToNull(filters.getPublicationTo()));
        safe.setCreationFrom(StringUtils.trimToNull(filters.getCreationFrom()));
        safe.setCreationTo(StringUtils.trimToNull(filters.getCreationTo()));
        safe.setModificationFrom(StringUtils.trimToNull(filters.getModificationFrom()));
        safe.setModificationTo(StringUtils.trimToNull(filters.getModificationTo()));
        safe.setAuthor(StringUtils.trimToNull(filters.getAuthor()));

        String contentType = StringUtils.trimToNull(filters.getContentType());
        if (contentType != null && !NODE_TYPE_PATTERN.matcher(contentType).matches()) {
            throw new IllegalArgumentException("Invalid content type");
        }

        safe.setContentType(contentType);
        return safe;
    }

    private GqlBulkEditSearchFiltersInput buildFilters(String text,
                                                       String path,
                                                       String contentType,
                                                       String publicationStatus,
                                                       String publicationFrom,
                                                       String publicationTo,
                                                       String creationFrom,
                                                       String creationTo,
                                                       String modificationFrom,
                                                       String modificationTo,
                                                       String author) {
        GqlBulkEditSearchFiltersInput filters = new GqlBulkEditSearchFiltersInput();
        filters.setText(text);
        filters.setPath(path);
        filters.setContentType(contentType);
        filters.setPublicationStatus(publicationStatus);
        filters.setPublicationFrom(publicationFrom);
        filters.setPublicationTo(publicationTo);
        filters.setCreationFrom(creationFrom);
        filters.setCreationTo(creationTo);
        filters.setModificationFrom(modificationFrom);
        filters.setModificationTo(modificationTo);
        filters.setAuthor(author);
        return filters;
    }

    private List<String> sanitizePropertyNames(List<String> properties) {
        if (properties == null) {
            return Collections.emptyList();
        }

        return properties.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(property -> PROPERTY_PATTERN.matcher(property).matches())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<GqlBulkEditUpdateInput> sanitizeUpdates(List<GqlBulkEditUpdateInput> propertyUpdates) {
        if (propertyUpdates == null) {
            return Collections.emptyList();
        }

        return propertyUpdates.stream()
                .filter(Objects::nonNull)
                .filter(update -> StringUtils.isNotBlank(update.getName()))
                .filter(update -> PROPERTY_PATTERN.matcher(update.getName()).matches())
                .filter(update -> StringUtils.isNotBlank(update.getValue()))
                .collect(Collectors.toList());
    }

    private List<GqlBulkEditUpdateInput> resolvePropertyUpdates(List<GqlBulkEditUpdateInput> propertyUpdates,
                                                                List<String> propertyNames,
                                                                List<String> propertyValues,
                                                                List<Boolean> propertyInternationalized) {
        if (propertyNames != null || propertyValues != null || propertyInternationalized != null) {
            return buildUpdatesFromArrays(propertyNames, propertyValues, propertyInternationalized);
        }

        return propertyUpdates;
    }

    private List<GqlBulkEditUpdateInput> buildUpdatesFromArrays(List<String> propertyNames,
                                                                List<String> propertyValues,
                                                                List<Boolean> propertyInternationalized) {
        if (propertyNames == null || propertyValues == null) {
            return Collections.emptyList();
        }

        int size = Math.min(propertyNames.size(), propertyValues.size());
        List<GqlBulkEditUpdateInput> updates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            GqlBulkEditUpdateInput update = new GqlBulkEditUpdateInput();
            update.setName(propertyNames.get(i));
            update.setValue(propertyValues.get(i));
            update.setI18n(propertyInternationalized != null && propertyInternationalized.size() > i && Boolean.TRUE.equals(propertyInternationalized.get(i)));
            updates.add(update);
        }

        return updates;
    }

    private List<String> sanitizeStringValues(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String buildSearchQuery(String siteKey, GqlBulkEditSearchFiltersInput filters) {
        StringBuilder query = new StringBuilder();
        String nodeType = filters != null && StringUtils.isNotBlank(filters.getContentType()) ? filters.getContentType() : "nt:base";
        String path = resolveSearchPath(siteKey, filters != null ? filters.getPath() : null);

        query.append("SELECT * FROM [").append(nodeType).append("] AS item WHERE ISDESCENDANTNODE(item,['")
                .append(escapeSqlLiteral(path))
                .append("'])");

        if (filters != null) {
            appendStatusConstraint(query, filters.getPublicationStatus());
            appendDateConstraint(query, "j:lastPublished", filters.getPublicationFrom(), filters.getPublicationTo());
            appendDateConstraint(query, "jcr:created", filters.getCreationFrom(), filters.getCreationTo());
            appendDateConstraint(query, "jcr:lastModified", filters.getModificationFrom(), filters.getModificationTo());

            if (StringUtils.isNotBlank(filters.getAuthor())) {
                String author = escapeSqlLiteral(filters.getAuthor());
                query.append(" AND (item.[jcr:createdBy] = '").append(author)
                        .append("' OR item.[jcr:lastModifiedBy] = '").append(author).append("')");
            }
        }

        query.append(" ORDER BY item.[jcr:lastModified] DESC");
        return query.toString();
    }

    private String resolveSearchPath(String siteKey, String path) {
        String siteRoot = "/sites/" + siteKey;
        String trimmedPath = StringUtils.trimToNull(path);
        if (trimmedPath == null) {
            return siteRoot;
        }

        if (!trimmedPath.startsWith("/")) {
            trimmedPath = siteRoot + "/" + trimmedPath;
        }

        if (!trimmedPath.startsWith(siteRoot)) {
            throw new IllegalArgumentException("The path must stay inside the current site");
        }

        return trimmedPath;
    }

    private void appendStatusConstraint(StringBuilder query, String publicationStatus) {
        if (StringUtils.isBlank(publicationStatus)) {
            return;
        }

        String normalized = publicationStatus.toLowerCase(Locale.ROOT);
        if ("published".equals(normalized)) {
            query.append(" AND item.[j:published] = true");
        } else if ("unpublished".equals(normalized)) {
            query.append(" AND (item.[j:published] IS NULL OR item.[j:published] = false)");
        }
    }

    private void appendDateConstraint(StringBuilder query, String propertyName, String from, String to) {
        if (StringUtils.isBlank(from) && StringUtils.isBlank(to)) {
            return;
        }

        if (StringUtils.isNotBlank(from)) {
            query.append(" AND item.[").append(propertyName).append("] >= CAST('")
                    .append(formatDateForQuery(from, false)).append("' AS DATE)");
        }

        if (StringUtils.isNotBlank(to)) {
            query.append(" AND item.[").append(propertyName).append("] <= CAST('")
                    .append(formatDateForQuery(to, true)).append("' AS DATE)");
        }
    }

    private String formatDateForQuery(String input, boolean endOfDay) {
        try {
            LocalDate parsed = LocalDate.parse(input, DATE_INPUT);
            if (endOfDay) {
                return parsed.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }

            return parsed.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date filter: " + input, e);
        }
    }

    private String escapeSqlLiteral(String value) {
        return StringUtils.defaultString(value).replace("'", "''");
    }

    private void collectCategories(
            JCRNodeWrapper parent,
            String language,
            List<GqlBulkEditCategoryInfo> categories,
            String parentCategoryIdentifier
    ) throws RepositoryException {
        NodeIterator iterator = parent.getNodes();
        while (iterator.hasNext()) {
            JCRNodeWrapper child = toNodeWrapper((JCRSessionWrapper) parent.getSession(), iterator.nextNode());
            if (child == null) {
                continue;
            }

            if (child.isNodeType("jnt:category")) {
                GqlBulkEditCategoryInfo categoryInfo = new GqlBulkEditCategoryInfo();
                categoryInfo.setIdentifier(child.getIdentifier());
                categoryInfo.setParentIdentifier(parentCategoryIdentifier);
                categoryInfo.setPath(child.getPath());
                categoryInfo.setName(child.getName());
                categoryInfo.setLabel(resolveDisplayName(child, language));
                categoryInfo.setHasChildren(child.hasNodes());
                categories.add(categoryInfo);

                collectCategories(child, language, categories, child.getIdentifier());
                continue;
            }

            collectCategories(child, language, categories, parentCategoryIdentifier);
        }
    }

    private GqlBulkEditNode buildSearchNode(JCRNodeWrapper node, String language, GqlBulkEditSearchFiltersInput filters, List<String> properties) throws RepositoryException {
        if (node == null) {
            return null;
        }

        JCRNodeWrapper translationNode = getTranslationNode(node, language);
        if (!matchesFilters(node, translationNode, language, filters)) {
            return null;
        }

        GqlBulkEditNode gqlNode = new GqlBulkEditNode();
        gqlNode.setUuid(node.getIdentifier());
        gqlNode.setPath(node.getPath());
        gqlNode.setName(node.getName());
        gqlNode.setDisplayName(resolveDisplayName(node, language));
        gqlNode.setNodeType(node.getPrimaryNodeTypeName());
        gqlNode.setNodeTypeLabel(node.getPrimaryNodeType().getName());
        gqlNode.setCreated(formatDate(resolveNodeDate(node, translationNode, "jcr:created")));
        gqlNode.setLastModified(formatDate(resolveNodeDate(node, translationNode, "jcr:lastModified")));
        gqlNode.setPublicationDate(formatDate(resolveNodeDate(node, translationNode, "j:lastPublished")));
        gqlNode.setPublicationStatus(isPublished(node) ? "published" : "unpublished");
        gqlNode.setAuthor(resolveAuthor(node, translationNode));
        gqlNode.setPropertyValues(collectPropertyValues(node, language, properties));
        gqlNode.setTags(readStringValues(node, "j:tagList"));
        gqlNode.setCategories(readReferencedNodeLabels(node, "j:defaultCategory", language));
        return gqlNode;
    }

    private boolean matchesFilters(JCRNodeWrapper node,
                                   JCRNodeWrapper translationNode,
                                   String language,
                                   GqlBulkEditSearchFiltersInput filters) throws RepositoryException {
        if (filters == null) {
            return true;
        }

        if (StringUtils.isNotBlank(filters.getPath())) {
            String normalizedPath = resolveSearchPath(extractSiteKeyFromPath(node.getPath()), filters.getPath());
            if (!node.getPath().startsWith(normalizedPath)) {
                return false;
            }
        }

        if (StringUtils.isNotBlank(filters.getContentType()) && !node.isNodeType(filters.getContentType())) {
            return false;
        }

        if (StringUtils.isNotBlank(filters.getPublicationStatus())) {
            boolean published = isPublished(node);
            if ("published".equalsIgnoreCase(filters.getPublicationStatus()) && !published) {
                return false;
            }

            if ("unpublished".equalsIgnoreCase(filters.getPublicationStatus()) && published) {
                return false;
            }
        }

        if (StringUtils.isNotBlank(filters.getAuthor())) {
            String author = resolveAuthor(node, translationNode);
            if (!StringUtils.equals(author, filters.getAuthor())) {
                return false;
            }
        }

        if (StringUtils.isNotBlank(filters.getText()) && !matchesTextSearch(node, language, filters.getText())) {
            return false;
        }

        if (!matchesDateFilter(resolveNodeDate(node, translationNode, "j:lastPublished"), filters.getPublicationFrom(), filters.getPublicationTo())) {
            return false;
        }

        if (!matchesDateFilter(resolveNodeDate(node, translationNode, "jcr:created"), filters.getCreationFrom(), filters.getCreationTo())) {
            return false;
        }

        return matchesDateFilter(resolveNodeDate(node, translationNode, "jcr:lastModified"), filters.getModificationFrom(), filters.getModificationTo());
    }

    private boolean matchesDateFilter(Calendar value, String from, String to) {
        if (StringUtils.isBlank(from) && StringUtils.isBlank(to)) {
            return true;
        }

        if (value == null) {
            return false;
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
        if (StringUtils.isNotBlank(from)) {
            LocalDateTime fromBoundary = LocalDate.parse(from, DATE_INPUT).atStartOfDay();
            if (dateTime.isBefore(fromBoundary)) {
                return false;
            }
        }

        if (StringUtils.isNotBlank(to)) {
            LocalDateTime toBoundary = LocalDate.parse(to, DATE_INPUT).atTime(23, 59, 59);
            if (dateTime.isAfter(toBoundary)) {
                return false;
            }
        }

        return true;
    }

    private String extractSiteKeyFromPath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }

        String[] segments = path.split("/");
        return segments.length > 2 ? segments[2] : "";
    }

    private String summarizeFilters(GqlBulkEditSearchFiltersInput filters) {
        if (filters == null) {
            return "{}";
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("text", filters.getText());
        summary.put("path", filters.getPath());
        summary.put("contentType", filters.getContentType());
        summary.put("publicationStatus", filters.getPublicationStatus());
        summary.put("publicationFrom", filters.getPublicationFrom());
        summary.put("publicationTo", filters.getPublicationTo());
        summary.put("creationFrom", filters.getCreationFrom());
        summary.put("creationTo", filters.getCreationTo());
        summary.put("modificationFrom", filters.getModificationFrom());
        summary.put("modificationTo", filters.getModificationTo());
        summary.put("author", filters.getAuthor());
        return summary.toString();
    }

    private boolean matchesTextSearch(JCRNodeWrapper node, String language, String term) throws RepositoryException {
        if (StringUtils.isBlank(term)) {
            return true;
        }

        JCRNodeWrapper translationNode = getTranslationNode(node, language);
        return containsInNode(node, term) || (translationNode != null && containsInNode(translationNode, term));
    }

    private boolean containsInNode(JCRNodeWrapper node, String term) {
        try {
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                if (property.getType() != PropertyType.STRING) {
                    continue;
                }

                if (property.isMultiple()) {
                    for (Value value : property.getValues()) {
                        if (value != null && containsIgnoreCase(value.getString(), term)) {
                            return true;
                        }
                    }
                    continue;
                }

                if (containsIgnoreCase(property.getString(), term)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to inspect node {}", node, e);
        }

        return containsIgnoreCase(node.getName(), term) || containsIgnoreCase(node.getPath(), term);
    }

    private boolean containsIgnoreCase(String value, String term) {
        return value != null && term != null && value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    private List<GqlBulkEditPropertyValue> collectPropertyValues(JCRNodeWrapper node, String language, List<String> propertyNames) throws RepositoryException {
        List<GqlBulkEditPropertyValue> values = new ArrayList<>();
        for (String propertyName : propertyNames) {
            if ("j:tagList".equals(propertyName) || "j:defaultCategory".equals(propertyName)) {
                continue;
            }

            Map<String, JCRNodeWrapper> holders = new LinkedHashMap<>();
            collectPropertyHoldersRecursively(node, language, propertyName, null, false, holders);
            JCRNodeWrapper holder = holders.values().stream().findFirst().orElse(null);
            GqlBulkEditPropertyValue propertyValue = new GqlBulkEditPropertyValue();
            propertyValue.setName(propertyName);

            if (holder == null || !holder.hasProperty(propertyName)) {
                propertyValue.setMultiple(false);
                propertyValue.setValue("");
                propertyValue.setValues(Collections.emptyList());
                values.add(propertyValue);
                continue;
            }

            Property property = holder.getProperty(propertyName);
            propertyValue.setMultiple(property.isMultiple());
            if (property.isMultiple()) {
                List<String> multiValues = Arrays.stream(property.getValues())
                        .map(value -> {
                            try {
                                return value != null ? value.getString() : "";
                            } catch (RepositoryException e) {
                                return "";
                            }
                        })
                        .collect(Collectors.toList());
                propertyValue.setValues(multiValues);
                propertyValue.setValue(String.join(", ", multiValues));
            } else {
                propertyValue.setValue(property.getString());
                propertyValue.setValues(Collections.singletonList(property.getString()));
            }

            values.add(propertyValue);
        }

        return values;
    }

    private List<String> readStringValues(JCRNodeWrapper node, String propertyName) throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            return Collections.emptyList();
        }

        Property property = node.getProperty(propertyName);
        if (property.isMultiple()) {
            return Arrays.stream(property.getValues())
                    .map(value -> {
                        try {
                            return value != null ? value.getString() : "";
                        } catch (RepositoryException e) {
                            return "";
                        }
                    })
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }

        return Collections.singletonList(property.getString());
    }

    private List<String> readReferencedNodeLabels(JCRNodeWrapper node, String propertyName, String language) throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            return Collections.emptyList();
        }

        List<String> labels = new ArrayList<>();
        Property property = node.getProperty(propertyName);
        if (property.isMultiple()) {
            for (Value value : property.getValues()) {
                if (value == null) {
                    continue;
                }

                try {
                    JCRNodeWrapper refNode = ((JCRSessionWrapper) node.getSession()).getNodeByIdentifier(value.getString());
                    labels.add(resolveDisplayName(refNode, language));
                } catch (Exception e) {
                    labels.add(value.getString());
                }
            }
            return labels;
        }

        try {
            JCRNodeWrapper refNode = ((JCRSessionWrapper) node.getSession()).getNodeByIdentifier(property.getString());
            labels.add(resolveDisplayName(refNode, language));
        } catch (Exception e) {
            labels.add(property.getString());
        }

        return labels;
    }

    private boolean isPublished(JCRNodeWrapper node) throws RepositoryException {
        return node.hasProperty("j:published") && node.getProperty("j:published").getBoolean();
    }

    private String resolveAuthor(JCRNodeWrapper node, JCRNodeWrapper translationNode) throws RepositoryException {
        List<String> candidateProperties = Arrays.asList("jcr:lastModifiedBy", "jcr:createdBy");
        for (String propertyName : candidateProperties) {
            if (translationNode != null && translationNode.hasProperty(propertyName)) {
                return translationNode.getProperty(propertyName).getString();
            }

            if (node.hasProperty(propertyName)) {
                return node.getProperty(propertyName).getString();
            }
        }

        return "";
    }

    private String resolveDisplayName(JCRNodeWrapper node, String language) throws RepositoryException {
        if (node == null) {
            return "";
        }

        JCRNodeWrapper translationNode = getTranslationNode(node, language);
        if (translationNode != null && translationNode.hasProperty("jcr:title")) {
            return StringUtils.defaultIfEmpty(translationNode.getProperty("jcr:title").getString(), node.getDisplayableName());
        }

        if (node.hasProperty("jcr:title")) {
            return StringUtils.defaultIfEmpty(node.getProperty("jcr:title").getString(), node.getDisplayableName());
        }

        return StringUtils.defaultIfEmpty(node.getDisplayableName(), node.getName());
    }

    private String formatDate(Calendar calendar) {
        if (calendar == null) {
            return "";
        }

        return DATE_OUTPUT.format(calendar.getTime());
    }

    private Calendar resolveNodeDate(JCRNodeWrapper node, JCRNodeWrapper translationNode, String propertyName) throws RepositoryException {
        if (translationNode != null && translationNode.hasProperty(propertyName)) {
            return translationNode.getProperty(propertyName).getDate();
        }

        if (node.hasProperty(propertyName)) {
            return node.getProperty(propertyName).getDate();
        }

        return null;
    }

    private JCRNodeWrapper getTranslationNode(JCRNodeWrapper node, String language) throws RepositoryException {
        if (node == null || StringUtils.isBlank(language)) {
            return null;
        }

        String translationNodeName = "j:translation_" + language.replace('-', '_');
        if (node.hasNode(translationNodeName)) {
            return node.getNode(translationNodeName);
        }

        String languageOnly = language.split("[-_]")[0];
        if (StringUtils.isNotBlank(languageOnly)) {
            String fallbackTranslationNodeName = "j:translation_" + languageOnly;
            if (node.hasNode(fallbackTranslationNodeName)) {
                return node.getNode(fallbackTranslationNodeName);
            }
        }

        return null;
    }

    private JCRNodeWrapper resolveSearchableNode(JCRNodeWrapper hitNode) throws RepositoryException {
        if (hitNode == null) {
            return null;
        }

        JCRNodeWrapper current = hitNode;
        while (current != null) {
            if (current.getName().startsWith("j:translation_")) {
                return current.getParent();
            }

            if ("/".equals(current.getPath()) || "/sites".equals(current.getPath())) {
                break;
            }

            current = current.getParent();
        }

        return hitNode;
    }

    private void ensureMixin(JCRNodeWrapper node, String mixinName) throws RepositoryException {
        if (node == null || StringUtils.isBlank(mixinName) || node.isNodeType(mixinName)) {
            return;
        }

        node.addMixin(mixinName);
    }

    private int updateStringProperty(JCRNodeWrapper node,
                                     String language,
                                     String propertyName,
                                     String value,
                                     boolean internationalized) throws RepositoryException {
        if (StringUtils.isBlank(propertyName) || StringUtils.isBlank(value)) {
            return 0;
        }

        if (internationalized) {
            Map<String, JCRNodeWrapper> holders = new LinkedHashMap<>();
            collectWritablePropertyHoldersRecursively(node, propertyName, value, true, holders);
            if (holders.isEmpty()) {
                return applyI18nPropertyUpdate(node, propertyName, value, language);
            }

            int updatedCount = 0;
            for (JCRNodeWrapper holder : holders.values()) {
                updatedCount += applyI18nPropertyUpdate(holder, propertyName, value, language);
            }

            return updatedCount;
        }

        Map<String, JCRNodeWrapper> existingHolders = new LinkedHashMap<>();
        collectPropertyHoldersRecursively(node, language, propertyName, value, false, existingHolders);

        if (!existingHolders.isEmpty()) {
            int updatedCount = 0;
            for (JCRNodeWrapper holder : existingHolders.values()) {
                updatedCount += applyStringPropertyUpdate(holder, propertyName, value);
            }

            return updatedCount;
        }

        return applyStringPropertyUpdate(node, propertyName, value);
    }

    private int applyI18nPropertyUpdate(JCRNodeWrapper targetNode,
                                        String propertyName,
                                        String value,
                                        String language) throws RepositoryException {
        if (targetNode == null || StringUtils.isBlank(propertyName) || StringUtils.isBlank(value) || StringUtils.isBlank(language)) {
            return 0;
        }

        targetNode.setProperty(propertyName, value, language);
        return 1;
    }

    private int applyStringPropertyUpdate(JCRNodeWrapper targetNode, String propertyName, String value) throws RepositoryException {
        if (targetNode == null || StringUtils.isBlank(propertyName) || StringUtils.isBlank(value)) {
            return 0;
        }

        if (targetNode.hasProperty(propertyName) && targetNode.getProperty(propertyName).isMultiple()) {
            String[] values = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .toArray(String[]::new);
            targetNode.setProperty(propertyName, values);
            return 1;
        }

        targetNode.setProperty(propertyName, value);
        return 1;
    }

    private void collectPropertyHoldersRecursively(JCRNodeWrapper currentNode,
                                                   String language,
                                                   String propertyName,
                                                   String value,
                                                   boolean includeApplicableTargets,
                                                   Map<String, JCRNodeWrapper> holders) throws RepositoryException {
        if (currentNode == null || StringUtils.isBlank(propertyName)) {
            return;
        }

        JCRNodeWrapper translationNode = getTranslationNode(currentNode, language);
        if (translationNode != null) {
            collectPropertyHoldersInNodeTree(translationNode, propertyName, value, includeApplicableTargets, holders);
        }

        addPropertyHolder(currentNode, propertyName, value, includeApplicableTargets, holders);

        NodeIterator children = currentNode.getNodes();
        while (children.hasNext()) {
            JCRNodeWrapper child = toNodeWrapper((JCRSessionWrapper) currentNode.getSession(), children.nextNode());
            if (child == null) {
                continue;
            }

            String childName = child.getName();
            if (childName != null && childName.startsWith("j:translation_")) {
                continue;
            }

            collectPropertyHoldersRecursively(child, language, propertyName, value, includeApplicableTargets, holders);
        }
    }

    private void collectPropertyHoldersInNodeTree(JCRNodeWrapper sourceNode,
                                                  String propertyName,
                                                  String value,
                                                  boolean includeApplicableTargets,
                                                  Map<String, JCRNodeWrapper> holders) throws RepositoryException {
        if (sourceNode == null || StringUtils.isBlank(propertyName)) {
            return;
        }

        addPropertyHolder(sourceNode, propertyName, value, includeApplicableTargets, holders);

        NodeIterator children = sourceNode.getNodes();
        while (children.hasNext()) {
            JCRNodeWrapper child = toNodeWrapper((JCRSessionWrapper) sourceNode.getSession(), children.nextNode());
            if (child == null) {
                continue;
            }

            collectPropertyHoldersInNodeTree(child, propertyName, value, includeApplicableTargets, holders);
        }
    }

    private void addPropertyHolder(JCRNodeWrapper node,
                                   String propertyName,
                                   String value,
                                   boolean includeApplicableTargets,
                                   Map<String, JCRNodeWrapper> holders) throws RepositoryException {
        if (node == null) {
            return;
        }

        if (node.hasProperty(propertyName)) {
            holders.putIfAbsent(node.getIdentifier(), node);
        }
    }

    private void collectWritablePropertyHoldersRecursively(JCRNodeWrapper currentNode,
                                                           String propertyName,
                                                           String value,
                                                           boolean includeApplicableTargets,
                                                           Map<String, JCRNodeWrapper> holders) throws RepositoryException {
        if (currentNode == null || StringUtils.isBlank(propertyName)) {
            return;
        }

        addPropertyHolder(currentNode, propertyName, value, includeApplicableTargets, holders);

        NodeIterator children = currentNode.getNodes();
        while (children.hasNext()) {
            JCRNodeWrapper child = toNodeWrapper((JCRSessionWrapper) currentNode.getSession(), children.nextNode());
            if (child == null) {
                continue;
            }

            String childName = child.getName();
            if (childName != null && childName.startsWith("j:translation_")) {
                continue;
            }

            collectWritablePropertyHoldersRecursively(child, propertyName, value, includeApplicableTargets, holders);
        }
    }

    private JCRNodeWrapper toNodeWrapper(JCRSessionWrapper session, Node node) throws RepositoryException {
        if (node instanceof JCRNodeWrapper) {
            return (JCRNodeWrapper) node;
        }

        return session.getNodeByIdentifier(node.getIdentifier());
    }
}
