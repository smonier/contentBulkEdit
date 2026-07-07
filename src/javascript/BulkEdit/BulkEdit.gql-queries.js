import {gql} from '@apollo/client';

export const GET_SITE_LANGUAGES_QUERY = gql`
    query GetSiteLanguages($workspace: Workspace!, $scope: String!) {
        jcr(workspace: $workspace) {
            nodeByPath(path: $scope) {
                languages: property(name: "j:languages") {
                    values
                }
            }
        }
    }
`;

export const GET_CONTENT_TYPES_QUERY = gql`
    query SiteContentTypesQuery($siteKey: String!, $language: String!) {
        jcr {
            nodeTypes(filter: {
                includeMixins: false,
                siteKey: $siteKey,
                includeTypes: ["jmix:editorialContent","jmix:mainResource", "jnt:page", "jnt:file"],
                excludeTypes: ["jmix:studioOnly", "jmix:hiddenType", "jnt:editableFile"]
            }) {
                nodes {
                    name
                    displayName(language: $language)
                    icon
                }
            }
        }
    }
`;

export const GET_PROPERTY_DEFINITIONS_QUERY = gql`
    query GetPropertyDefinitionsQuery($type: String!, $language: String!) {
        contentBulkEdit {
            getPropertyDefinitions(nodeType: $type, language: $language) {
                name
                label
                requiredType
                selectorType
                declaringNodeType
                declaringNodeTypeLabel
                internationalized
                multiple
                mandatory
                constraints
                defaultValues
                selectorOptions {
                    name
                    value
                }
            }
        }
    }
`;

export const GET_CATEGORIES_QUERY = gql`
    query GetCategories($siteKey: String!, $language: String!, $rootPath: String) {
        contentBulkEdit {
            getCategories(siteKey: $siteKey, language: $language, rootPath: $rootPath) {
                identifier
                parentIdentifier
                path
                name
                label
                hasChildren
            }
        }
    }
`;

export const SEARCH_CONTENT_QUERY = gql`
    query SearchContent(
        $siteKey: String!
        $language: String!
        $text: String
        $path: String
        $contentType: String
        $publicationStatus: String
        $publicationFrom: String
        $publicationTo: String
        $creationFrom: String
        $creationTo: String
        $modificationFrom: String
        $modificationTo: String
        $author: String
        $properties: [String!]
        $limit: Int
    ) {
        contentBulkEdit {
            searchContent(
                siteKey: $siteKey
                language: $language
                text: $text
                path: $path
                contentType: $contentType
                publicationStatus: $publicationStatus
                publicationFrom: $publicationFrom
                publicationTo: $publicationTo
                creationFrom: $creationFrom
                creationTo: $creationTo
                modificationFrom: $modificationFrom
                modificationTo: $modificationTo
                author: $author
                properties: $properties
                limit: $limit
            ) {
                totalCount
                truncated
                nodes {
                    uuid
                    path
                    name
                    displayName
                    nodeType
                    nodeTypeLabel
                    publicationStatus
                    publicationDate
                    created
                    lastModified
                    author
                    tags
                    categories
                    propertyValues {
                        name
                        value
                        values
                        multiple
                    }
                }
            }
        }
    }
`;

export const BULK_EDIT_MUTATION = gql`
    mutation BulkEditContent(
        $siteKey: String!
        $language: String!
        $nodeUuids: [String!]!
        $propertyNames: [String!]
        $propertyValues: [String!]
        $propertyModes: [String!]
        $clearPropertyNames: [String!]
        $tagValues: [String!]
        $tagMode: String
        $categoryIdentifiers: [String!]
        $categoryMode: String
    ) {
        contentBulkEdit {
            bulkEditContent(
                siteKey: $siteKey
                language: $language
                nodeUuids: $nodeUuids
                propertyNames: $propertyNames
                propertyValues: $propertyValues
                propertyModes: $propertyModes
                clearPropertyNames: $clearPropertyNames
                tagValues: $tagValues
                tagMode: $tagMode
                categoryIdentifiers: $categoryIdentifiers
                categoryMode: $categoryMode
            ) {
                successfulNodes
                failedNodes
                updatedProperties
                errors {
                    nodeUuid
                    nodePath
                    message
                }
            }
        }
    }
`;
