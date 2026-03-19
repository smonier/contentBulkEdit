import React, {useEffect, useMemo, useState} from 'react';
import {useLazyQuery, useMutation, useQuery} from '@apollo/client';
import {Button, Header, LayoutContent, Paper, Typography} from '@jahia/moonstone';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import {
    BULK_EDIT_MUTATION,
    GET_ALL_USERS_QUERY,
    GET_CATEGORIES_QUERY,
    GET_CONTENT_PROPERTIES_QUERY,
    GET_CONTENT_TYPES_QUERY,
    GET_SITE_LANGUAGES_QUERY,
    SEARCH_CONTENT_QUERY
} from './BulkEdit.gql-queries';
import {BulkEditPanel} from './components/BulkEditPanel';
import {ConfirmDialog} from './components/ConfirmDialog';
import {PropertySelector} from './components/PropertySelector';
import {ResultsTable} from './components/ResultsTable';
import {SearchFilters} from './components/SearchFilters';
import styles from './BulkEdit.module.scss';

const defaultFilters = {
    text: '',
    path: '',
    contentType: '',
    publicationStatus: '',
    publicationFrom: '',
    publicationTo: '',
    creationFrom: '',
    creationTo: '',
    modificationFrom: '',
    modificationTo: '',
    author: ''
};

const notify = (type, message) => {
    if (window?.jahia?.ui?.notify) {
        window.jahia.ui.notify(type, null, message);
    }
};

export const BulkEdit = ({match}) => {
    const {t, i18n} = useTranslation('contentBulkEdit');
    const siteKey = match?.params?.siteKey || window?.contextJsParameters?.siteKey || '';
    const sitePath = `/sites/${siteKey}`;

    const [selectedLanguage, setSelectedLanguage] = useState((window?.contextJsParameters?.uilang || i18n.language || 'en').split('-')[0]);
    const [filters, setFilters] = useState(defaultFilters);
    const [selectedProperties, setSelectedProperties] = useState([]);
    const [selectedNodes, setSelectedNodes] = useState([]);
    const [bulkValues, setBulkValues] = useState({});
    const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
    const [selectedCategoryLabels, setSelectedCategoryLabels] = useState([]);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [lastSearchVariables, setLastSearchVariables] = useState(null);
    const [searchValidationErrors, setSearchValidationErrors] = useState({});

    const {data: languagesData} = useQuery(GET_SITE_LANGUAGES_QUERY, {
        variables: {workspace: 'EDIT', scope: sitePath},
        skip: !siteKey
    });

    const {data: contentTypesData, loading: loadingTypes} = useQuery(GET_CONTENT_TYPES_QUERY, {
        variables: {siteKey, language: selectedLanguage},
        skip: !siteKey
    });

    const {data: authorsData} = useQuery(GET_ALL_USERS_QUERY);
    const {data: categoryData} = useQuery(GET_CATEGORIES_QUERY, {
        variables: {siteKey, language: selectedLanguage},
        skip: !siteKey
    });

    const {data: propertiesData, loading: loadingProperties} = useQuery(GET_CONTENT_PROPERTIES_QUERY, {
        variables: {type: filters.contentType, language: selectedLanguage},
        skip: !filters.contentType
    });

    const [executeSearch, {data: searchData, loading: searching}] = useLazyQuery(SEARCH_CONTENT_QUERY, {
        fetchPolicy: 'network-only'
    });

    const [executeBulkEdit, {loading: executing}] = useMutation(BULK_EDIT_MUTATION);

    const languages = useMemo(() => {
        return languagesData?.jcr?.nodeByPath?.languages?.values || [];
    }, [languagesData]);

    useEffect(() => {
        if (languages.length > 0 && !languages.includes(selectedLanguage)) {
            setSelectedLanguage(languages[0]);
        }
    }, [languages, selectedLanguage]);

    useEffect(() => {
        if (!lastSearchVariables) {
            return;
        }

        const previousProperties = lastSearchVariables.properties || [];
        const sameProperties = previousProperties.length === selectedProperties.length &&
            previousProperties.every((property, index) => property === selectedProperties[index]);

        if (sameProperties) {
            return;
        }

        const nextVariables = {
            ...lastSearchVariables,
            properties: selectedProperties
        };

        setLastSearchVariables(nextVariables);
        executeSearch({variables: nextVariables});
    }, [executeSearch, lastSearchVariables, selectedProperties]);

    const contentTypes = useMemo(() => {
        const nodes = contentTypesData?.jcr?.nodeTypes?.nodes || [];
        return nodes
            .filter(Boolean)
            .map(node => ({
                label: node.displayName || node.name,
                value: node.name
            }))
            .sort((left, right) => left.label.localeCompare(right.label));
    }, [contentTypesData]);

    const authors = useMemo(() => {
        const nodes = authorsData?.jcr?.nodesByQuery?.nodes || [];
        return nodes
            .filter(Boolean)
            .map(node => ({
                label: node.property?.value ? `${node.name} (${node.property.value})` : node.name,
                value: node.name
            }))
            .sort((left, right) => left.label.localeCompare(right.label));
    }, [authorsData]);

    const properties = useMemo(() => {
        const nodes = propertiesData?.jcr?.nodeTypes?.nodes || [];
        const rawProperties = nodes[0]?.properties || [];
        return rawProperties
            .filter(property => property && !property.hidden && !property.protected && !['j:tagList', 'j:defaultCategory'].includes(property.name))
            .map(property => ({
                name: property.name,
                label: property.displayName || property.name,
                i18n: Boolean(property.internationalized),
                multiple: Boolean(property.multiple)
            }))
            .sort((left, right) => left.label.localeCompare(right.label));
    }, [propertiesData]);

    const propertyLabels = useMemo(() => {
        return properties.reduce((acc, property) => {
            acc[property.name] = property.label;
            return acc;
        }, {
            'j:tagList': t('contentBulkEdit.table.tags'),
            'j:defaultCategory': t('contentBulkEdit.table.category')
        });
    }, [properties, t]);

    const propertyDefinitions = useMemo(() => {
        return properties.reduce((acc, property) => {
            acc[property.name] = property;
            return acc;
        }, {});
    }, [properties]);

    const categoryTreeData = useMemo(() => categoryData?.contentBulkEdit?.getCategories || [], [categoryData]);
    const nodes = useMemo(() => searchData?.contentBulkEdit?.searchContent?.nodes || [], [searchData]);
    const searchResultCount = searchData?.contentBulkEdit?.searchContent?.totalCount || 0;
    const searchTruncated = Boolean(searchData?.contentBulkEdit?.searchContent?.truncated);

    const handleFilterChange = (name, value) => {
        setFilters(prev => ({...prev, [name]: value}));
        setSearchValidationErrors(prev => {
            if (!prev[name]) {
                return prev;
            }

            const next = {...prev};
            delete next[name];
            return next;
        });

        if (name === 'contentType') {
            setLastSearchVariables(null);
            setSelectedProperties([]);
            setSelectedNodes([]);
            setBulkValues({});
            setSelectedCategoryIds([]);
            setSelectedCategoryLabels([]);
        }
    };

    const handleToggleProperty = (propertyName, checked) => {
        setSelectedProperties(prev => checked ? [...prev, propertyName] : prev.filter(item => item !== propertyName));
    };

    const handleSearch = () => {
        if (!filters.contentType) {
            setSearchValidationErrors({contentType: true});
            notify('warning', t('contentBulkEdit.selectType'));
            return;
        }

        setSearchValidationErrors({});

        const variables = {
            siteKey,
            language: selectedLanguage,
            text: filters.text || null,
            path: filters.path || null,
            contentType: filters.contentType || null,
            publicationStatus: filters.publicationStatus || null,
            publicationFrom: filters.publicationFrom || null,
            publicationTo: filters.publicationTo || null,
            creationFrom: filters.creationFrom || null,
            creationTo: filters.creationTo || null,
            modificationFrom: filters.modificationFrom || null,
            modificationTo: filters.modificationTo || null,
            author: filters.author || null,
            properties: selectedProperties,
            limit: 200
        };

        setLastSearchVariables(variables);
        setSelectedNodes([]);
        executeSearch({variables});
    };

    const handleClear = () => {
        setFilters(defaultFilters);
        setLastSearchVariables(null);
        setSelectedProperties([]);
        setSelectedNodes([]);
        setBulkValues({});
        setSelectedCategoryIds([]);
        setSelectedCategoryLabels([]);
        setSearchValidationErrors({});
    };

    const handleToggleAll = checked => {
        setSelectedNodes(checked ? nodes.map(node => node.uuid) : []);
    };

    const handleToggleRow = (uuid, checked) => {
        setSelectedNodes(prev => checked ? [...prev, uuid] : prev.filter(item => item !== uuid));
    };

    const handleBulkValueChange = (name, value) => {
        setBulkValues(prev => ({...prev, [name]: value}));
    };

    const handleCategoryChange = (identifiers, labels) => {
        setSelectedCategoryIds(identifiers);
        setSelectedCategoryLabels(labels);
    };

    const updates = useMemo(() => {
        return selectedProperties
            .filter(property => Boolean(bulkValues[property]))
            .map(property => ({
                name: property,
                label: propertyLabels[property] || property,
                value: bulkValues[property],
                i18n: Boolean(propertyDefinitions[property]?.i18n)
            }));
    }, [bulkValues, propertyDefinitions, propertyLabels, selectedProperties]);

    const tagValues = useMemo(() => {
        return (bulkValues.tags || '')
            .split(',')
            .map(item => item.trim())
            .filter(Boolean);
    }, [bulkValues.tags]);

    const canExecute = selectedNodes.length > 0 && (updates.length > 0 || tagValues.length > 0 || selectedCategoryIds.length > 0);

    const summary = {
        count: selectedNodes.length,
        properties: updates,
        tags: tagValues,
        categories: selectedCategoryLabels
    };

    const handleOpenConfirm = () => {
        if (selectedNodes.length === 0) {
            notify('warning', t('contentBulkEdit.selectRows'));
            return;
        }

        if (!canExecute) {
            notify('warning', t('contentBulkEdit.selectValues'));
            return;
        }

        setConfirmOpen(true);
    };

    const handleExecute = async () => {
        try {
            const response = await executeBulkEdit({
                variables: {
                    siteKey,
                    language: selectedLanguage,
                    nodeUuids: selectedNodes,
                    propertyNames: updates.map(update => update.name),
                    propertyValues: updates.map(update => update.value),
                    propertyInternationalized: updates.map(update => update.i18n),
                    tagValues: tagValues.length > 0 ? tagValues : null,
                    categoryIdentifiers: selectedCategoryIds.length > 0 ? selectedCategoryIds : null
                }
            });

            const result = response?.data?.contentBulkEdit?.bulkEditContent;
            setConfirmOpen(false);

            if (result?.failedNodes?.length > 0) {
                notify('warning', t('contentBulkEdit.partialSuccess'));
            } else {
                notify('success', t('contentBulkEdit.success'));
            }

            setSelectedNodes([]);
            setBulkValues({});
            setSelectedCategoryIds([]);
            setSelectedCategoryLabels([]);

            if (lastSearchVariables) {
                executeSearch({variables: lastSearchVariables});
            }
        } catch {
            notify('error', t('contentBulkEdit.error'));
        }
    };

    return (
        <>
            <Header
                title={t('contentBulkEdit.header', {siteInfo: siteKey})}
                mainActions={[
                    <Button
                        key="clear-search"
                        label={t('contentBulkEdit.clear')}
                        variant="ghost"
                        onClick={handleClear}
                    />,
                    <Button
                        key="run-search"
                        label={t('contentBulkEdit.search')}
                        onClick={handleSearch}
                    />,
                    <Button
                        key="bulk-apply"
                        label={t('contentBulkEdit.apply')}
                        color="accent"
                        isDisabled={!canExecute}
                        onClick={handleOpenConfirm}
                    />
                ]}
            />

            <LayoutContent content={(
                <div className={styles.root}>
                    <SearchFilters
                        t={t}
                        filters={filters}
                        validationErrors={searchValidationErrors}
                        contentTypes={contentTypes}
                        authors={authors}
                        languages={languages.length > 0 ? languages : [selectedLanguage]}
                        selectedLanguage={selectedLanguage}
                        siteKey={siteKey}
                        onFilterChange={handleFilterChange}
                        onLanguageChange={setSelectedLanguage}
                    />

                    <PropertySelector
                        t={t}
                        isLoading={loadingProperties || loadingTypes}
                        properties={properties}
                        selectedProperties={selectedProperties}
                        onToggleProperty={handleToggleProperty}
                    />

                    <div className={styles.resultsWorkspace}>
                        <Paper className={`${styles.panel} ${styles.resultsPanel}`}>
                            <div className={styles.panelHeader}>
                                <Typography variant="heading" weight="bold">
                                    {t('contentBulkEdit.resultsTitle')}
                                </Typography>
                                <Typography variant="body">
                                    {t('contentBulkEdit.resultsSummary', {
                                        results: searchResultCount,
                                        selected: selectedNodes.length
                                    })}
                                </Typography>
                            </div>

                            {searchTruncated && (
                                <Typography variant="caption" className={styles.warningText}>
                                    {t('contentBulkEdit.searchLimitWarning')}
                                </Typography>
                            )}

                            {searching && (
                                <Typography variant="body">{t('contentBulkEdit.loading')}</Typography>
                            )}

                            {!searching && nodes.length === 0 && (
                                <Typography variant="body">{t('contentBulkEdit.noResults')}</Typography>
                            )}

                            {!searching && nodes.length > 0 && (
                                <ResultsTable
                                    t={t}
                                    nodes={nodes}
                                    selectedProperties={selectedProperties}
                                    propertyLabels={propertyLabels}
                                    selectedNodes={selectedNodes}
                                    onToggleAll={handleToggleAll}
                                    onToggleRow={handleToggleRow}
                                />
                            )}
                        </Paper>

                        <BulkEditPanel
                            t={t}
                            selectedProperties={selectedProperties}
                            propertyLabels={propertyLabels}
                            bulkValues={bulkValues}
                            categoryData={categoryTreeData}
                            selectedCategoryIds={selectedCategoryIds}
                            selectedCategoryLabels={selectedCategoryLabels}
                            selectedRowsCount={selectedNodes.length}
                            onBulkValueChange={handleBulkValueChange}
                            onCategoryChange={handleCategoryChange}
                        />
                    </div>
                </div>
            )}/>

            <ConfirmDialog
                t={t}
                isOpen={confirmOpen}
                summary={summary}
                isLoading={executing}
                onCancel={() => setConfirmOpen(false)}
                onConfirm={handleExecute}
            />
        </>
    );
};

BulkEdit.propTypes = {
    match: PropTypes.object
};
