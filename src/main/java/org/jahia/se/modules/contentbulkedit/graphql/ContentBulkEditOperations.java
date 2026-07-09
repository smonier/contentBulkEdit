package org.jahia.se.modules.contentbulkedit.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditCategoryInfo;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditExecutionError;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditExecutionResult;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditNode;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditPropertyDefinition;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditPropertyValue;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditSearchFiltersInput;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditSearchResult;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditSelectorOption;
import org.jahia.se.modules.contentbulkedit.graphql.model.GqlBulkEditUpdateInput;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.ComplexPublicationService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.PublicationInfo;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.content.nodetypes.SelectorType;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GraphQL operations backing the Content Bulk Edit admin panel.
 *
 * <p>All operations run with the <em>calling user's</em> JCR session on the {@code default}
 * workspace, so JCR ACLs apply naturally to both reads (search) and writes (bulk edit).
 * Guest access is rejected up front.</p>
 *
 * <p>Property writes are definition-driven: the applicable {@link ExtendedPropertyDefinition}
 * decides internationalization, cardinality and value type (weak references, dates, booleans,
 * numbers) — the client only ever sends string representations.</p>
 */
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
    private static final String FORM_FIELDSETS_PATH = "/META-INF/jahia-content-editor-forms/fieldsets";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JCRTemplate jcrTemplate;
    private ComplexPublicationService complexPublicationService;

    @Inject
    @GraphQLOsgiService
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Inject
    @GraphQLOsgiService
    public void setComplexPublicationService(ComplexPublicationService complexPublicationService) {
        this.complexPublicationService = complexPublicationService;
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
        JahiaUser user = requireAuthenticatedUser();

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
        final Locale locale = toLocale(normalizedLanguage);

        return jcrTemplate.doExecute(user, Constants.EDIT_WORKSPACE, locale, session -> {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String searchStatement = buildSearchQuery(validatedSiteKey, safeFilters);
            logger.info("contentBulkEdit searchContent user={} siteKey={} language={} filters={} query={}",
                    user.getName(), validatedSiteKey, normalizedLanguage, summarizeFilters(safeFilters), searchStatement);
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
    @GraphQLDescription("Retrieve categories as a flat tree, optionally scoped to a subtree")
    public List<GqlBulkEditCategoryInfo> getCategories(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("rootPath") @GraphQLDescription("Category subtree root; defaults to the system categories root") String rootPath
    ) throws RepositoryException {
        ensureJcrTemplate();
        JahiaUser user = requireAuthenticatedUser();
        validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);
        final String safeRootPath = resolveCategoryRoot(rootPath);

        return jcrTemplate.doExecute(user, Constants.EDIT_WORKSPACE, toLocale(normalizedLanguage), session -> {
            if (!session.nodeExists(safeRootPath)) {
                return Collections.emptyList();
            }

            JCRNodeWrapper root = session.getNode(safeRootPath);
            List<GqlBulkEditCategoryInfo> categories = new ArrayList<>();
            collectCategories(root, normalizedLanguage, categories, null);
            return categories;
        });
    }

    private String resolveCategoryRoot(String rootPath) {
        String trimmed = StringUtils.trimToNull(rootPath);
        if (trimmed == null) {
            return CATEGORY_ROOT;
        }

        if (trimmed.contains("..") || !(trimmed.equals(CATEGORY_ROOT) || trimmed.startsWith(CATEGORY_ROOT + "/"))) {
            throw new IllegalArgumentException("Category root must be under " + CATEGORY_ROOT);
        }

        return trimmed;
    }

    @GraphQLField
    @GraphQLName("getPropertyDefinitions")
    @GraphQLDescription("Editable property definitions of a node type with full type metadata (required type, selector, constraints). Requires jContent access on the site.")
    public List<GqlBulkEditPropertyDefinition> getPropertyDefinitions(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("nodeType") @GraphQLNonNull String nodeType,
            @GraphQLName("language") String language
    ) throws RepositoryException {
        ensureJcrTemplate();
        JahiaUser user = requireAuthenticatedUser();
        final String validatedSiteKey = validateSiteKey(siteKey);

        String trimmedType = StringUtils.trimToNull(nodeType);
        if (trimmedType == null || !NODE_TYPE_PATTERN.matcher(trimmedType).matches()) {
            throw new IllegalArgumentException("Invalid node type");
        }

        final Locale locale = toLocale(normalizeLanguage(language));

        // Content-model metadata is editor-facing: gate it on the same permission that
        // grants access to jContent (where this panel lives) on the requested site.
        jcrTemplate.doExecute(user, Constants.EDIT_WORKSPACE, locale, session -> {
            JCRNodeWrapper siteNode;
            try {
                siteNode = session.getNode("/sites/" + validatedSiteKey);
            } catch (javax.jcr.PathNotFoundException e) {
                throw new IllegalArgumentException("Unknown site: " + validatedSiteKey);
            }

            if (!siteNode.hasPermission("jContentAccess")) {
                throw new SecurityException("Insufficient permissions to browse property definitions on site " + validatedSiteKey);
            }

            return null;
        });

        ExtendedNodeType type;
        try {
            type = NodeTypeRegistry.getInstance().getNodeType(trimmedType);
        } catch (NoSuchNodeTypeException e) {
            throw new IllegalArgumentException("Unknown node type: " + trimmedType);
        }

        // Only expose the property model of editorial content types (the same set the
        // Content Bulk Edit UI offers) so the endpoint cannot enumerate arbitrary system
        // types (e.g. jnt:user) for any authenticated user.
        if (!isEditorialContentType(type)) {
            throw new IllegalArgumentException("Property definitions are only available for editorial content types");
        }

        Map<String, Map<String, FormFieldOverride>> formOverrides = loadFormFieldOverrides();

        // Collect one definition per property name, preferring the most specific declaring
        // type when a property is redeclared: exact type > non-mixin supertype > mixin.
        Map<String, ExtendedPropertyDefinition> byName = new LinkedHashMap<>();
        for (ExtendedPropertyDefinition propertyDefinition : type.getPropertyDefinitions()) {
            if (propertyDefinition.isHidden() || propertyDefinition.isProtected()) {
                continue;
            }

            ExtendedPropertyDefinition existing = byName.get(propertyDefinition.getName());
            if (existing == null || declarationPriority(propertyDefinition, trimmedType) > declarationPriority(existing, trimmedType)) {
                byName.put(propertyDefinition.getName(), propertyDefinition);
            }
        }

        // Order like content editor: the type's own fields first, then inherited non-mixin
        // fields, then one group per declaring mixin, preserving declaration order throughout.
        List<GqlBulkEditPropertyDefinition> ownDefinitions = new ArrayList<>();
        List<GqlBulkEditPropertyDefinition> inheritedDefinitions = new ArrayList<>();
        Map<String, List<GqlBulkEditPropertyDefinition>> mixinGroups = new LinkedHashMap<>();

        for (ExtendedPropertyDefinition propertyDefinition : byName.values()) {
            GqlBulkEditPropertyDefinition definition = toGqlPropertyDefinition(propertyDefinition, locale);
            applyFormOverride(definition, propertyDefinition, formOverrides);

            ExtendedNodeType declaringType = propertyDefinition.getDeclaringNodeType();
            if (declaringType.getName().equals(trimmedType)) {
                ownDefinitions.add(definition);
            } else if (declaringType.isMixin()) {
                mixinGroups.computeIfAbsent(declaringType.getName(), key -> new ArrayList<>()).add(definition);
            } else {
                inheritedDefinitions.add(definition);
            }
        }

        List<GqlBulkEditPropertyDefinition> result = new ArrayList<>(ownDefinitions);
        result.addAll(inheritedDefinitions);
        mixinGroups.values().forEach(result::addAll);
        return result;
    }

    private boolean isEditorialContentType(ExtendedNodeType type) {
        return type.isNodeType("jmix:editorialContent")
                || type.isNodeType("jmix:mainResource")
                || type.isNodeType("jnt:page")
                || type.isNodeType("jnt:file");
    }

    private int declarationPriority(ExtendedPropertyDefinition propertyDefinition, String requestedType) {
        ExtendedNodeType declaringType = propertyDefinition.getDeclaringNodeType();
        if (declaringType.getName().equals(requestedType)) {
            return 2;
        }

        return declaringType.isMixin() ? 0 : 1;
    }

    @GraphQLField
    @GraphQLDescription("Execute a bulk edit on selected nodes")
    public GqlBulkEditExecutionResult bulkEditContent(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("nodeUuids") @GraphQLNonNull List<String> nodeUuids,
            @GraphQLName("propertyNames") List<String> propertyNames,
            @GraphQLName("propertyValues") List<String> propertyValues,
            @GraphQLName("propertyModes") @GraphQLDescription("Per property, replace (default) or append; append only applies to multi-valued properties") List<String> propertyModes,
            @GraphQLName("clearPropertyNames") @GraphQLDescription("Properties to remove from the selected nodes") List<String> clearPropertyNames,
            @GraphQLName("tagValues") List<String> tagValues,
            @GraphQLName("tagMode") @GraphQLDescription("replace (default) or append") String tagMode,
            @GraphQLName("categoryIdentifiers") List<String> categoryIdentifiers,
            @GraphQLName("categoryMode") @GraphQLDescription("replace (default) or append") String categoryMode
    ) throws RepositoryException {
        ensureJcrTemplate();
        JahiaUser user = requireAuthenticatedUser();

        final String validatedSiteKey = validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);
        final List<String> safeClears = sanitizePropertyNames(clearPropertyNames);
        // A property flagged for clearing wins over a value update for the same name
        final List<GqlBulkEditUpdateInput> safeUpdates = sanitizeUpdates(buildUpdatesFromArrays(propertyNames, propertyValues, propertyModes)).stream()
                .filter(update -> !safeClears.contains(update.getName()))
                .collect(Collectors.toList());
        // Validate modes up front so a typo fails the whole call, not individual nodes
        safeUpdates.forEach(update -> isAppendMode(update.getMode()));
        final List<String> safeTags = sanitizeStringValues(tagValues);
        final List<String> safeCategoryIdentifiers = sanitizeStringValues(categoryIdentifiers);
        final boolean appendTags = isAppendMode(tagMode);
        final boolean appendCategories = isAppendMode(categoryMode);

        Locale locale = toLocale(normalizedLanguage);
        return jcrTemplate.doExecute(user, Constants.EDIT_WORKSPACE, locale, session -> {
            GqlBulkEditExecutionResult executionResult = new GqlBulkEditExecutionResult();
            executionResult.setSuccessfulNodes(new ArrayList<>());
            executionResult.setFailedNodes(new ArrayList<>());
            executionResult.setErrors(new ArrayList<>());
            executionResult.setUpdatedProperties(0);

            if (safeUpdates.isEmpty() && safeClears.isEmpty() && safeTags.isEmpty() && safeCategoryIdentifiers.isEmpty()) {
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

                    if (!node.hasPermission("jcr:write")) {
                        throw new RepositoryException("Insufficient permissions to modify " + node.getPath());
                    }

                    int updatedCount = 0;
                    for (GqlBulkEditUpdateInput update : safeUpdates) {
                        updatedCount += applyPropertyUpdate(node, update.getName(), update.getValue(), isAppendMode(update.getMode()));
                    }

                    for (String clearName : safeClears) {
                        updatedCount += clearProperty(node, clearName);
                    }

                    if (!safeTags.isEmpty()) {
                        ensureMixin(node, "jmix:tagged");
                        List<String> tags = appendTags
                                ? mergeValues(readStringValues(node, "j:tagList"), safeTags)
                                : safeTags;
                        node.setProperty("j:tagList", tags.toArray(new String[0]));
                        updatedCount++;
                    }

                    if (!safeCategoryIdentifiers.isEmpty()) {
                        ensureMixin(node, "jmix:categorized");
                        List<String> categories = appendCategories
                                ? mergeValues(readStringValues(node, "j:defaultCategory"), safeCategoryIdentifiers)
                                : safeCategoryIdentifiers;
                        node.setProperty("j:defaultCategory", categories.toArray(new String[0]));
                        updatedCount++;
                    }

                    // Save per node so a failure never leaves half-applied changes
                    // from another node in the shared session.
                    session.save();

                    executionResult.getSuccessfulNodes().add(nodeUuid);
                    executionResult.setUpdatedProperties(executionResult.getUpdatedProperties() + updatedCount);
                } catch (Exception e) {
                    logger.error("Bulk edit failed on node {}", nodeUuid, e);
                    try {
                        session.refresh(false);
                    } catch (RepositoryException refreshError) {
                        logger.warn("Unable to roll back session after failure on node {}", nodeUuid, refreshError);
                    }

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

            return executionResult;
        });
    }

    @GraphQLField
    @GraphQLDescription("Publish selected nodes (with their references) in the given language")
    public GqlBulkEditExecutionResult publishContent(
            @GraphQLName("siteKey") @GraphQLNonNull String siteKey,
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("nodeUuids") @GraphQLNonNull List<String> nodeUuids
    ) throws RepositoryException {
        ensureJcrTemplate();
        JahiaUser user = requireAuthenticatedUser();

        final String validatedSiteKey = validateSiteKey(siteKey);
        final String normalizedLanguage = normalizeLanguage(language);
        final JCRPublicationService publicationService = ServicesRegistry.getInstance().getJCRPublicationService();
        final Set<String> languages = Collections.singleton(normalizedLanguage);

        Locale locale = toLocale(normalizedLanguage);
        return jcrTemplate.doExecute(user, Constants.EDIT_WORKSPACE, locale, session -> {
            GqlBulkEditExecutionResult executionResult = new GqlBulkEditExecutionResult();
            executionResult.setSuccessfulNodes(new ArrayList<>());
            executionResult.setFailedNodes(new ArrayList<>());
            executionResult.setErrors(new ArrayList<>());
            executionResult.setUpdatedProperties(0);

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

                    // publishByMainId escalates internally, so the caller must gate on the
                    // publish permission explicitly.
                    if (!node.hasPermission("publish")) {
                        throw new RepositoryException("Insufficient permissions to publish " + node.getPath());
                    }

                    publicationService.publishByMainId(nodeUuid, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE,
                            languages, false, Collections.emptyList());

                    executionResult.getSuccessfulNodes().add(nodeUuid);
                    executionResult.setUpdatedProperties(executionResult.getUpdatedProperties() + 1);
                } catch (Exception e) {
                    logger.error("Bulk publish failed on node {}", nodeUuid, e);
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

            return executionResult;
        });
    }

    private void ensureJcrTemplate() {
        if (jcrTemplate == null) {
            throw new IllegalStateException("JCRTemplate service is unavailable");
        }
    }

    /**
     * Rejects anonymous access. All operations rely on the calling user's session so that
     * JCR ACLs apply; guests must never reach a bulk edit entry point.
     */
    private JahiaUser requireAuthenticatedUser() {
        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        if (user == null || Constants.GUEST_USERNAME.equals(user.getName())) {
            throw new SecurityException("Content bulk edit requires an authenticated user");
        }

        return user;
    }

    private Locale toLocale(String normalizedLanguage) {
        return LanguageCodeConverters.languageCodeToLocale(normalizedLanguage.replace('_', '-'));
    }

    private boolean isAppendMode(String mode) {
        String trimmed = StringUtils.trimToEmpty(mode);
        if (trimmed.isEmpty() || "replace".equalsIgnoreCase(trimmed)) {
            return false;
        }

        if ("append".equalsIgnoreCase(trimmed)) {
            return true;
        }

        throw new IllegalArgumentException("Invalid mode '" + mode + "': expected 'replace' or 'append'");
    }

    /**
     * Union of existing and new values, preserving order (existing first) and dropping duplicates.
     */
    private List<String> mergeValues(List<String> existing, List<String> additions) {
        Set<String> merged = new LinkedHashSet<>();
        if (existing != null) {
            merged.addAll(existing);
        }

        merged.addAll(additions);
        return new ArrayList<>(merged);
    }

    private GqlBulkEditPropertyDefinition toGqlPropertyDefinition(ExtendedPropertyDefinition propertyDefinition, Locale locale) {
        GqlBulkEditPropertyDefinition definition = new GqlBulkEditPropertyDefinition();
        definition.setName(propertyDefinition.getName());

        String label = null;
        try {
            label = propertyDefinition.getLabel(locale);
        } catch (Exception e) {
            logger.debug("Unable to resolve label for property {}", propertyDefinition.getName(), e);
        }

        definition.setLabel(StringUtils.defaultIfBlank(label, propertyDefinition.getName()));
        definition.setRequiredType(PropertyType.nameFromValue(propertyDefinition.getRequiredType()));

        ExtendedNodeType declaringType = propertyDefinition.getDeclaringNodeType();
        definition.setDeclaringNodeType(declaringType.getName());
        String declaringLabel = null;
        try {
            declaringLabel = declaringType.getLabel(locale);
        } catch (Exception e) {
            logger.debug("Unable to resolve label for node type {}", declaringType.getName(), e);
        }

        definition.setDeclaringNodeTypeLabel(StringUtils.defaultIfBlank(declaringLabel, declaringType.getName()));

        String selectorName = null;
        try {
            selectorName = SelectorType.nameFromValue(propertyDefinition.getSelector());
        } catch (Exception e) {
            logger.debug("Unknown selector type {} for property {}", propertyDefinition.getSelector(), propertyDefinition.getName());
        }

        definition.setSelectorType(selectorName);

        List<GqlBulkEditSelectorOption> selectorOptions = new ArrayList<>();
        Map<String, String> rawOptions = propertyDefinition.getSelectorOptions();
        if (rawOptions != null) {
            for (Map.Entry<String, String> entry : rawOptions.entrySet()) {
                GqlBulkEditSelectorOption option = new GqlBulkEditSelectorOption();
                option.setName(entry.getKey());
                option.setValue(entry.getValue());
                selectorOptions.add(option);
            }
        }

        definition.setSelectorOptions(selectorOptions);

        String[] valueConstraints = propertyDefinition.getValueConstraints();
        definition.setConstraints(valueConstraints != null ? Arrays.asList(valueConstraints) : Collections.emptyList());

        List<String> defaultValues = new ArrayList<>();
        Value[] rawDefaults = propertyDefinition.getDefaultValues();
        if (rawDefaults != null) {
            for (Value defaultValue : rawDefaults) {
                try {
                    defaultValues.add(defaultValue.getString());
                } catch (Exception e) {
                    logger.debug("Skipping unreadable default value on property {}", propertyDefinition.getName());
                }
            }
        }

        definition.setDefaultValues(defaultValues);
        definition.setInternationalized(propertyDefinition.isInternationalized());
        definition.setMultiple(propertyDefinition.isMultiple());
        definition.setMandatory(propertyDefinition.isMandatory());
        return definition;
    }

    /**
     * Scans active bundles for content-editor fieldset overrides
     * ({@code META-INF/jahia-content-editor-forms/fieldsets/*.json}) and indexes them by
     * fieldset (node type) name, then field name. These overrides carry UI metadata that
     * does not exist in the CND, e.g. {@code selectorType: ChoiceTree} with a
     * {@code rootPath} scoping a category picker to a subtree.
     */
    private Map<String, Map<String, FormFieldOverride>> loadFormFieldOverrides() {
        Map<String, Map<String, FormFieldOverride>> overrides = new LinkedHashMap<>();
        Bundle currentBundle = FrameworkUtil.getBundle(getClass());
        if (currentBundle == null || currentBundle.getBundleContext() == null) {
            return overrides;
        }

        for (Bundle bundle : currentBundle.getBundleContext().getBundles()) {
            if (bundle.getState() != Bundle.ACTIVE) {
                continue;
            }

            Enumeration<URL> entries = bundle.findEntries(FORM_FIELDSETS_PATH, "*.json", false);
            if (entries == null) {
                continue;
            }

            while (entries.hasMoreElements()) {
                URL entry = entries.nextElement();
                try (InputStream stream = entry.openStream()) {
                    JsonNode root = OBJECT_MAPPER.readTree(stream);
                    String fieldSetName = root.path("name").asText(null);
                    if (StringUtils.isBlank(fieldSetName)) {
                        continue;
                    }

                    Map<String, FormFieldOverride> fields = overrides.computeIfAbsent(fieldSetName, key -> new LinkedHashMap<>());
                    for (JsonNode field : root.path("fields")) {
                        String fieldName = field.path("name").asText(null);
                        if (StringUtils.isBlank(fieldName)) {
                            continue;
                        }

                        FormFieldOverride override = new FormFieldOverride();
                        override.selectorType = field.path("selectorType").asText(null);
                        JsonNode optionsNode = field.path("selectorOptionsMap");
                        if (optionsNode.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> optionFields = optionsNode.fields();
                            while (optionFields.hasNext()) {
                                Map.Entry<String, JsonNode> option = optionFields.next();
                                override.selectorOptions.put(option.getKey(), option.getValue().asText());
                            }
                        }

                        fields.put(fieldName, override);
                    }
                } catch (IOException e) {
                    logger.warn("Unable to parse content editor form override {} from bundle {}", entry, bundle.getSymbolicName(), e);
                }
            }
        }

        return overrides;
    }

    private void applyFormOverride(GqlBulkEditPropertyDefinition definition,
                                   ExtendedPropertyDefinition propertyDefinition,
                                   Map<String, Map<String, FormFieldOverride>> formOverrides) {
        Map<String, FormFieldOverride> fieldSet = formOverrides.get(propertyDefinition.getDeclaringNodeType().getName());
        if (fieldSet == null) {
            return;
        }

        FormFieldOverride override = fieldSet.get(propertyDefinition.getName());
        if (override == null) {
            return;
        }

        if (StringUtils.isNotBlank(override.selectorType)) {
            definition.setSelectorType(override.selectorType);
        }

        if (!override.selectorOptions.isEmpty()) {
            Map<String, String> merged = new LinkedHashMap<>();
            for (GqlBulkEditSelectorOption option : definition.getSelectorOptions()) {
                merged.put(option.getName(), option.getValue());
            }

            merged.putAll(override.selectorOptions);

            List<GqlBulkEditSelectorOption> options = new ArrayList<>(merged.size());
            for (Map.Entry<String, String> entry : merged.entrySet()) {
                GqlBulkEditSelectorOption option = new GqlBulkEditSelectorOption();
                option.setName(entry.getKey());
                option.setValue(entry.getValue());
                options.add(option);
            }

            definition.setSelectorOptions(options);
        }
    }

    private static final class FormFieldOverride {
        private String selectorType;
        private final Map<String, String> selectorOptions = new LinkedHashMap<>();
    }

    /**
     * Applies one bulk value to a node, driven entirely by the applicable property
     * definition: internationalization is handled by the localized session, cardinality
     * by {@link ExtendedPropertyDefinition#isMultiple()}, and the string value is coerced
     * to the definition's required JCR type before writing. For multi-valued properties,
     * append mode merges the new values into the existing ones (duplicates dropped);
     * for single-valued properties the mode is ignored.
     *
     * @return 1 if the property was written, 0 if it was skipped
     */
    private int applyPropertyUpdate(JCRNodeWrapper node, String propertyName, String rawValue, boolean append) throws RepositoryException {
        if (StringUtils.isBlank(propertyName) || StringUtils.isBlank(rawValue)) {
            return 0;
        }

        ExtendedPropertyDefinition definition = node.getApplicablePropertyDefinition(propertyName);
        if (definition == null) {
            logger.warn("No applicable definition for property {} on {}; skipping", propertyName, node.getPath());
            return 0;
        }

        if (definition.isProtected()) {
            logger.warn("Property {} on {} is protected; skipping", propertyName, node.getPath());
            return 0;
        }

        JCRSessionWrapper session = node.getSession();
        if (definition.isMultiple()) {
            List<String> rawValues = Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());

            if (append && node.hasProperty(propertyName)) {
                Property existingProperty = node.getProperty(propertyName);
                List<String> existing = new ArrayList<>();
                if (existingProperty.isMultiple()) {
                    for (Value existingValue : existingProperty.getValues()) {
                        if (existingValue != null) {
                            existing.add(existingValue.getString());
                        }
                    }
                }

                rawValues = mergeValues(existing, rawValues);
            }

            List<Value> values = new ArrayList<>(rawValues.size());
            for (String part : rawValues) {
                values.add(coerceValue(session, definition, part));
            }

            node.setProperty(propertyName, values.toArray(new Value[0]));
            return 1;
        }

        node.setProperty(propertyName, coerceValue(session, definition, rawValue));
        return 1;
    }

    /**
     * Removes a property from the node (the localized session resolves i18n properties to
     * the current language's translation node). Clearing a mandatory property fails at
     * save time and is reported as a per-node error.
     *
     * @return 1 if the property was removed, 0 if it was absent or protected
     */
    private int clearProperty(JCRNodeWrapper node, String propertyName) throws RepositoryException {
        if (StringUtils.isBlank(propertyName)) {
            return 0;
        }

        ExtendedPropertyDefinition definition = node.getApplicablePropertyDefinition(propertyName);
        if (definition != null && definition.isProtected()) {
            logger.warn("Property {} on {} is protected; skipping clear", propertyName, node.getPath());
            return 0;
        }

        if (!node.hasProperty(propertyName)) {
            return 0;
        }

        node.getProperty(propertyName).remove();
        return 1;
    }

    /**
     * Converts the client-supplied string into a typed JCR {@link Value} matching the
     * definition's required type. References accept a UUID or an absolute path.
     */
    private Value coerceValue(JCRSessionWrapper session, ExtendedPropertyDefinition definition, String rawValue) throws RepositoryException {
        ValueFactory valueFactory = session.getValueFactory();
        switch (definition.getRequiredType()) {
            case PropertyType.WEAKREFERENCE:
            case PropertyType.REFERENCE:
                JCRNodeWrapper target = rawValue.startsWith("/")
                        ? session.getNode(rawValue)
                        : session.getNodeByIdentifier(rawValue);
                return valueFactory.createValue(target, definition.getRequiredType() == PropertyType.WEAKREFERENCE);
            case PropertyType.DATE:
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis(parseInstantValue(rawValue).toEpochMilli());
                return valueFactory.createValue(calendar);
            case PropertyType.BOOLEAN:
                return valueFactory.createValue(Boolean.parseBoolean(rawValue));
            case PropertyType.LONG:
                return valueFactory.createValue(Long.parseLong(rawValue));
            case PropertyType.DOUBLE:
                return valueFactory.createValue(Double.parseDouble(rawValue));
            case PropertyType.DECIMAL:
                return valueFactory.createValue(new BigDecimal(rawValue));
            default:
                return valueFactory.createValue(rawValue);
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

    private List<GqlBulkEditUpdateInput> buildUpdatesFromArrays(List<String> propertyNames, List<String> propertyValues, List<String> propertyModes) {
        if (propertyNames == null || propertyValues == null) {
            return Collections.emptyList();
        }

        int size = Math.min(propertyNames.size(), propertyValues.size());
        List<GqlBulkEditUpdateInput> updates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            GqlBulkEditUpdateInput update = new GqlBulkEditUpdateInput();
            update.setName(propertyNames.get(i));
            update.setValue(propertyValues.get(i));
            update.setMode(propertyModes != null && propertyModes.size() > i ? propertyModes.get(i) : null);
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

    /**
     * Builds the JCR-SQL2 statement. Type, path, full-text and date-range filters are all
     * pushed into the query so Jackrabbit's index does the narrowing; only publication
     * status and author remain post-filtered in Java.
     */
    private String buildSearchQuery(String siteKey, GqlBulkEditSearchFiltersInput filters) {
        StringBuilder query = new StringBuilder();
        String nodeType = filters != null && StringUtils.isNotBlank(filters.getContentType()) ? filters.getContentType() : "nt:base";
        String path = resolveSearchPath(siteKey, filters != null ? filters.getPath() : null);

        query.append("SELECT * FROM [").append(nodeType).append("] AS item WHERE ISDESCENDANTNODE(item,['")
                .append(escapeSqlLiteral(path))
                .append("'])");

        if (filters != null) {
            String fullTextExpression = StringUtils.isNotBlank(filters.getText())
                    ? escapeFullTextExpression(filters.getText())
                    : null;
            if (StringUtils.isNotBlank(fullTextExpression)) {
                query.append(" AND CONTAINS(item.*, '")
                        .append(escapeSqlLiteral(fullTextExpression))
                        .append("')");
            }

            appendDateRange(query, "j:lastPublished", filters.getPublicationFrom(), filters.getPublicationTo());
            appendDateRange(query, "jcr:created", filters.getCreationFrom(), filters.getCreationTo());
            appendDateRange(query, "jcr:lastModified", filters.getModificationFrom(), filters.getModificationTo());
        }

        query.append(" ORDER BY item.[jcr:lastModified] DESC");
        return query.toString();
    }

    /**
     * Appends an inclusive [from, to] day-range condition on a date property.
     * Bounds are UTC day boundaries; the upper bound is exclusive of the next day's start.
     */
    private void appendDateRange(StringBuilder query, String propertyName, String from, String to) {
        if (StringUtils.isNotBlank(from)) {
            LocalDate fromDate = parseInputDate(from);
            query.append(" AND item.[").append(propertyName).append("] >= CAST('")
                    .append(fromDate.format(DATE_INPUT)).append("T00:00:00.000Z' AS DATE)");
        }

        if (StringUtils.isNotBlank(to)) {
            LocalDate toDate = parseInputDate(to).plusDays(1);
            query.append(" AND item.[").append(propertyName).append("] < CAST('")
                    .append(toDate.format(DATE_INPUT)).append("T00:00:00.000Z' AS DATE)");
        }
    }

    /**
     * Makes user input safe for Jackrabbit's full-text expression parser: metacharacters
     * with structural meaning are blanked out, the remaining operators are escaped so the
     * term is matched literally.
     */
    private String escapeFullTextExpression(String term) {
        String neutralized = term.replaceAll("[(){}\\[\\]:^~!?]", " ");
        return neutralized.replace("\\", "\\\\").replace("\"", "\\\"").replace("-", "\\-").trim();
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

        if (!(trimmedPath.equals(siteRoot) || trimmedPath.startsWith(siteRoot + "/"))) {
            throw new IllegalArgumentException("The path must stay inside the current site");
        }

        return trimmedPath;
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
        String publicationStatus = resolvePublicationStatus(node, translationNode, language);
        if (!matchesFilters(node, translationNode, filters, publicationStatus)) {
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
        gqlNode.setPublicationStatus(publicationStatus);
        gqlNode.setAuthor(resolveAuthor(node, translationNode));
        gqlNode.setPropertyValues(collectPropertyValues(node, language, properties));
        gqlNode.setTags(readStringValues(node, "j:tagList"));
        gqlNode.setCategories(readReferencedNodeLabels(node, "j:defaultCategory", language));
        return gqlNode;
    }

    private boolean matchesFilters(JCRNodeWrapper node,
                                   JCRNodeWrapper translationNode,
                                   GqlBulkEditSearchFiltersInput filters,
                                   String publicationStatus) throws RepositoryException {
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

        if (StringUtils.isNotBlank(filters.getPublicationStatus()) &&
                !filters.getPublicationStatus().equalsIgnoreCase(publicationStatus)) {
            return false;
        }

        if (StringUtils.isNotBlank(filters.getAuthor())) {
            String author = resolveAuthor(node, translationNode);
            if (!StringUtils.equals(author, filters.getAuthor())) {
                return false;
            }
        }

        // Text and date filters are enforced by the JCR-SQL2 query itself.
        return true;
    }

    private LocalDate parseInputDate(String input) {
        try {
            return LocalDate.parse(input, DATE_INPUT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date filter: " + input, e);
        }
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

    /**
     * Reads the current values of the requested properties directly from the node.
     * The session is localized, so i18n properties resolve transparently; reference
     * properties are rendered as the target node's display name.
     */
    private List<GqlBulkEditPropertyValue> collectPropertyValues(JCRNodeWrapper node, String language, List<String> propertyNames) throws RepositoryException {
        List<GqlBulkEditPropertyValue> values = new ArrayList<>();
        for (String propertyName : propertyNames) {
            if ("j:tagList".equals(propertyName) || "j:defaultCategory".equals(propertyName)) {
                continue;
            }

            GqlBulkEditPropertyValue propertyValue = new GqlBulkEditPropertyValue();
            propertyValue.setName(propertyName);

            if (!node.hasProperty(propertyName)) {
                propertyValue.setMultiple(false);
                propertyValue.setValue("");
                propertyValue.setValues(Collections.emptyList());
                values.add(propertyValue);
                continue;
            }

            Property property = node.getProperty(propertyName);
            propertyValue.setMultiple(property.isMultiple());
            if (property.isMultiple()) {
                List<String> multiValues = new ArrayList<>();
                for (Value value : property.getValues()) {
                    multiValues.add(renderValue(node, value, language));
                }

                propertyValue.setValues(multiValues);
                propertyValue.setValue(String.join(", ", multiValues));
            } else {
                String rendered = renderValue(node, property.getValue(), language);
                propertyValue.setValue(rendered);
                propertyValue.setValues(Collections.singletonList(rendered));
            }

            values.add(propertyValue);
        }

        return values;
    }

    private String renderValue(JCRNodeWrapper node, Value value, String language) {
        if (value == null) {
            return "";
        }

        try {
            if (value.getType() == PropertyType.REFERENCE || value.getType() == PropertyType.WEAKREFERENCE) {
                try {
                    JCRNodeWrapper target = ((JCRSessionWrapper) node.getSession()).getNodeByIdentifier(value.getString());
                    return resolveDisplayName(target, language);
                } catch (Exception e) {
                    return value.getString();
                }
            }

            return value.getString();
        } catch (RepositoryException e) {
            return "";
        }
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

    /**
     * Publication status resolved by Jahia's publication engine (same source as jContent's
     * status badges), so non-i18n property changes, translation changes, and pending
     * deletions are all detected. States: {@code unpublished}, {@code published},
     * {@code modified}, {@code markedForDeletion}.
     */
    private String resolvePublicationStatus(JCRNodeWrapper node, JCRNodeWrapper translationNode, String language) throws RepositoryException {
        if (complexPublicationService != null) {
            try {
                ComplexPublicationService.AggregatedPublicationInfo info = complexPublicationService.getAggregatedPublicationInfo(
                        node.getIdentifier(), language.split("[-_]")[0], false, false, (JCRSessionWrapper) node.getSession());
                return mapPublicationStatus(info.getPublicationStatus(), node);
            } catch (Exception e) {
                logger.debug("Unable to resolve aggregated publication info for {}", node.getPath(), e);
            }
        }

        return resolvePublicationStatusFromDates(node, translationNode);
    }

    private String mapPublicationStatus(int status, JCRNodeWrapper node) throws RepositoryException {
        switch (status) {
            case PublicationInfo.PUBLISHED:
            case PublicationInfo.LIVE_ONLY:
                return "published";
            case PublicationInfo.MODIFIED:
            case PublicationInfo.LIVE_MODIFIED:
            case PublicationInfo.CONFLICT:
                return "modified";
            case PublicationInfo.NOT_PUBLISHED:
                return "notPublished";
            case PublicationInfo.UNPUBLISHED:
                return "unpublished";
            case PublicationInfo.MARKED_FOR_DELETION:
            case PublicationInfo.DELETED:
                return "markedForDeletion";
            default:
                return isPublished(node) ? "published" : resolveNeverOrUnpublished(node);
        }
    }

    /**
     * Distinguishes content that was never published from content that was published
     * then taken offline: a {@code j:lastPublished} property only exists after a
     * first publication.
     */
    private String resolveNeverOrUnpublished(JCRNodeWrapper node) throws RepositoryException {
        return node.hasProperty("j:lastPublished") ? "unpublished" : "notPublished";
    }

    /**
     * Date-based fallback when the publication service is unavailable. Compares the most
     * recent modification (main node or translation) against the last publication.
     */
    private String resolvePublicationStatusFromDates(JCRNodeWrapper node, JCRNodeWrapper translationNode) throws RepositoryException {
        if (!isPublished(node)) {
            return resolveNeverOrUnpublished(node);
        }

        Calendar lastModified = latestDate(node, translationNode, "jcr:lastModified");
        Calendar lastPublished = latestDate(node, translationNode, "j:lastPublished");
        if (lastModified != null && lastPublished != null && lastModified.after(lastPublished)) {
            return "modified";
        }

        return "published";
    }

    private Calendar latestDate(JCRNodeWrapper node, JCRNodeWrapper translationNode, String propertyName) throws RepositoryException {
        Calendar mainDate = node.hasProperty(propertyName) ? readCalendarProperty(node.getProperty(propertyName)) : null;
        Calendar translationDate = translationNode != null && translationNode.hasProperty(propertyName)
                ? readCalendarProperty(translationNode.getProperty(propertyName))
                : null;

        if (mainDate == null) {
            return translationDate;
        }

        if (translationDate == null) {
            return mainDate;
        }

        return translationDate.after(mainDate) ? translationDate : mainDate;
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
            Calendar date = readCalendarProperty(translationNode.getProperty(propertyName));
            if (date != null) {
                return date;
            }
        }

        if (node.hasProperty(propertyName)) {
            return readCalendarProperty(node.getProperty(propertyName));
        }

        return null;
    }

    private Calendar readCalendarProperty(Property property) throws RepositoryException {
        if (property == null) {
            return null;
        }

        try {
            return property.getDate();
        } catch (ValueFormatException e) {
            String rawValue = StringUtils.trimToNull(property.getString());
            if (rawValue == null) {
                return null;
            }

            Instant instant = parseInstantValue(rawValue);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(instant.toEpochMilli());
            return calendar;
        }
    }

    private Instant parseInstantValue(String rawValue) {
        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            // Try other common Jahia/JCR string date representations below.
        }

        try {
            return OffsetDateTime.parse(rawValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try local date-time and date-only representations below.
        }

        try {
            return LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Try date-only representation below.
        }

        try {
            return LocalDate.parse(rawValue, DATE_INPUT).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid repository date value: " + rawValue, e);
        }
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

    private JCRNodeWrapper toNodeWrapper(JCRSessionWrapper session, Node node) throws RepositoryException {
        if (node instanceof JCRNodeWrapper) {
            return (JCRNodeWrapper) node;
        }

        return session.getNodeByIdentifier(node.getIdentifier());
    }
}
