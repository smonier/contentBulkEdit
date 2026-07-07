import React, {useEffect, useMemo, useState} from 'react';
import {useLazyQuery, useMutation, useQuery} from '@apollo/client';
import {Button, Header, LayoutContent, Paper, Typography} from '@jahia/moonstone';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import {
    BULK_EDIT_MUTATION,
    GET_CATEGORIES_QUERY,
    GET_PROPERTY_DEFINITIONS_QUERY,
    GET_CONTENT_TYPES_QUERY,
    GET_SITE_LANGUAGES_QUERY,
    SEARCH_CONTENT_QUERY
} from './BulkEdit.gql-queries';
import {BulkEditPanel} from './components/BulkEditPanel';
import {ConfirmDialog} from './components/ConfirmDialog';
import {PropertySelector} from './components/PropertySelector';
import {ResultsTable} from './components/ResultsTable';
import {SearchFilters} from './components/SearchFilters';
import {SearchSummaryBar} from './components/SearchSummaryBar';
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
    const [clearFlags, setClearFlags] = useState({});
    const [propertyModes, setPropertyModes] = useState({});
    const [tagMode, setTagMode] = useState('replace');
    const [categoryMode, setCategoryMode] = useState('replace');
    const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
    const [selectedCategoryLabels, setSelectedCategoryLabels] = useState([]);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [lastSearchVariables, setLastSearchVariables] = useState(null);
    const [searchValidationErrors, setSearchValidationErrors] = useState({});
    const [filtersCollapsed, setFiltersCollapsed] = useState(false);

    const {data: languagesData} = useQuery(GET_SITE_LANGUAGES_QUERY, {
        variables: {workspace: 'EDIT', scope: sitePath},
        skip: !siteKey
    });

    const {data: contentTypesData, loading: loadingTypes} = useQuery(GET_CONTENT_TYPES_QUERY, {
        variables: {siteKey, language: selectedLanguage},
        skip: !siteKey
    });

    const {data: categoryData} = useQuery(GET_CATEGORIES_QUERY, {
        variables: {siteKey, language: selectedLanguage},
        skip: !siteKey
    });

    const {data: propertiesData, loading: loadingProperties} = useQuery(GET_PROPERTY_DEFINITIONS_QUERY, {
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

    useEffect(() => {
        if (!lastSearchVariables) {
            return;
        }

        if (!filters.contentType) {
            return;
        }

        const nextVariables = {
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

        setLastSearchVariables(nextVariables);
        executeSearch({variables: nextVariables});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filters, selectedLanguage]);

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

    const properties = useMemo(() => {
        const rawProperties = propertiesData?.contentBulkEdit?.getPropertyDefinitions || [];
        return rawProperties
            .filter(property => property && !['j:tagList', 'j:defaultCategory'].includes(property.name))
            .map(property => ({
                name: property.name,
                label: property.label || property.name,
                i18n: Boolean(property.internationalized),
                multiple: Boolean(property.multiple),
                mandatory: Boolean(property.mandatory),
                requiredType: property.requiredType,
                selectorType: property.selectorType,
                declaringNodeType: property.declaringNodeType,
                declaringNodeTypeLabel: property.declaringNodeTypeLabel,
                selectorOptions: property.selectorOptions || [],
                constraints: property.constraints || [],
                defaultValues: property.defaultValues || []
            }));
        // Server order mirrors content editor: own fields, inherited fields, then mixins.
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
            setClearFlags({});
            setPropertyModes({});
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
        setFiltersCollapsed(true);
        executeSearch({variables});
    };

    const handleClear = () => {
        setFilters(defaultFilters);
        setLastSearchVariables(null);
        setSelectedProperties([]);
        setSelectedNodes([]);
        setBulkValues({});
        setClearFlags({});
        setPropertyModes({});
        setTagMode('replace');
        setCategoryMode('replace');
        setSelectedCategoryIds([]);
        setSelectedCategoryLabels([]);
        setSearchValidationErrors({});
        setFiltersCollapsed(false);
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

    const handleToggleClear = (name, checked) => {
        setClearFlags(prev => ({...prev, [name]: checked}));
    };

    const handlePropertyModeChange = (name, mode) => {
        setPropertyModes(prev => ({...prev, [name]: mode}));
    };

    const handleCategoryChange = (identifiers, labels) => {
        setSelectedCategoryIds(identifiers);
        setSelectedCategoryLabels(labels);
    };

    const clearedProperties = useMemo(() => {
        return selectedProperties
            .filter(property => Boolean(clearFlags[property]))
            .map(property => ({
                name: property,
                label: propertyLabels[property] || property
            }));
    }, [clearFlags, propertyLabels, selectedProperties]);

    const updates = useMemo(() => {
        return selectedProperties
            .filter(property => !clearFlags[property] && Boolean(bulkValues[property]))
            .map(property => {
                const rawValue = bulkValues[property];
                const isPickerValue = typeof rawValue === 'object' && rawValue !== null;
                return {
                    name: property,
                    label: propertyLabels[property] || property,
                    value: isPickerValue ? rawValue.uuid : rawValue,
                    displayValue: isPickerValue ? rawValue.label : rawValue,
                    mode: propertyModes[property] || 'replace'
                };
            })
            .filter(update => Boolean(update.value));
    }, [bulkValues, clearFlags, propertyLabels, propertyModes, selectedProperties]);

    const tagValues = useMemo(() => {
        return (bulkValues.tags || '')
            .split(',')
            .map(item => item.trim())
            .filter(Boolean);
    }, [bulkValues.tags]);

    const changesCount = updates.length + clearedProperties.length +
        (tagValues.length > 0 ? 1 : 0) + (selectedCategoryIds.length > 0 ? 1 : 0);
    const canExecute = selectedNodes.length > 0 && changesCount > 0;

    const selectedTypeLabel = contentTypes.find(type => type.value === filters.contentType)?.label || filters.contentType;

    const summary = {
        count: selectedNodes.length,
        properties: updates,
        clears: clearedProperties,
        tags: tagValues,
        tagMode,
        categories: selectedCategoryLabels,
        categoryMode
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
                    propertyModes: updates.map(update => update.mode),
                    clearPropertyNames: clearedProperties.length > 0 ? clearedProperties.map(cleared => cleared.name) : null,
                    tagValues: tagValues.length > 0 ? tagValues : null,
                    tagMode,
                    categoryIdentifiers: selectedCategoryIds.length > 0 ? selectedCategoryIds : null,
                    categoryMode
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
            setClearFlags({});
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
                    {filtersCollapsed ? (
                        <SearchSummaryBar
                            t={t}
                            filters={filters}
                            typeLabel={selectedTypeLabel}
                            language={selectedLanguage}
                            resultsCount={searchResultCount}
                            selectedCount={selectedNodes.length}
                            onEdit={() => setFiltersCollapsed(false)}
                        />
                    ) : (
                        <SearchFilters
                            t={t}
                            filters={filters}
                            validationErrors={searchValidationErrors}
                            contentTypes={contentTypes}
                            languages={languages.length > 0 ? languages : [selectedLanguage]}
                            selectedLanguage={selectedLanguage}
                            siteKey={siteKey}
                            onFilterChange={handleFilterChange}
                            onLanguageChange={setSelectedLanguage}
                        />
                    )}

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
                            siteKey={siteKey}
                            selectedProperties={selectedProperties}
                            propertyLabels={propertyLabels}
                            propertyDefinitions={propertyDefinitions}
                            selectedLanguage={selectedLanguage}
                            bulkValues={bulkValues}
                            clearFlags={clearFlags}
                            propertyModes={propertyModes}
                            tagMode={tagMode}
                            categoryMode={categoryMode}
                            categoryData={categoryTreeData}
                            selectedCategoryIds={selectedCategoryIds}
                            selectedCategoryLabels={selectedCategoryLabels}
                            selectedRowsCount={selectedNodes.length}
                            changesCount={changesCount}
                            isApplyEnabled={canExecute}
                            onBulkValueChange={handleBulkValueChange}
                            onToggleClear={handleToggleClear}
                            onPropertyModeChange={handlePropertyModeChange}
                            onTagModeChange={setTagMode}
                            onCategoryModeChange={setCategoryMode}
                            onCategoryChange={handleCategoryChange}
                            onApply={handleOpenConfirm}
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
