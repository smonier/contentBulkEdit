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
                includeTypes: ["jmix:mainResource", "jnt:page", "jnt:file"],
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

export const GET_CONTENT_PROPERTIES_QUERY = gql`
    query GetContentPropertiesQuery($type: String!, $language: String!) {
        jcr {
            nodeTypes(filter: {includeTypes: [$type]}) {
                nodes {
                    properties(fieldFilter: {filters: [{fieldName: "hidden", value: "false"}]}) {
                        name
                        hidden
                        displayName(language: $language)
                        internationalized
                        mandatory
                        protected
                        multiple
                    }
                }
            }
        }
    }
`;

export const GET_ALL_USERS_QUERY = gql`
    query GetAllUsers {
        jcr {
            nodesByQuery(
                query: "SELECT * FROM [jnt:user]"
                queryLanguage: SQL2
            ) {
                nodes {
                    name
                    property(name: "j:email") {
                        value
                    }
                }
            }
        }
    }
`;

export const GET_CATEGORIES_QUERY = gql`
    query GetCategories($siteKey: String!, $language: String!) {
        contentBulkEdit {
            getCategories(siteKey: $siteKey, language: $language) {
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
        $propertyInternationalized: [Boolean!]
        $tagValues: [String!]
        $categoryIdentifiers: [String!]
    ) {
        contentBulkEdit {
            bulkEditContent(
                siteKey: $siteKey
                language: $language
                nodeUuids: $nodeUuids
                propertyNames: $propertyNames
                propertyValues: $propertyValues
                propertyInternationalized: $propertyInternationalized
                tagValues: $tagValues
                categoryIdentifiers: $categoryIdentifiers
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
