import React, {useMemo} from 'react';
import PropTypes from 'prop-types';
import {Button, Dropdown, Input, Paper, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

export const SearchFilters = ({
    t,
    filters,
    validationErrors,
    contentTypes,
    authors,
    languages,
    selectedLanguage,
    siteKey,
    onFilterChange,
    onLanguageChange
}) => {
    const publicationStatuses = useMemo(() => ([
        {label: t('contentBulkEdit.allStatuses'), value: ''},
        {label: t('contentBulkEdit.published'), value: 'published'},
        {label: t('contentBulkEdit.unpublished'), value: 'unpublished'}
    ]), [t]);

    const contentTypeOptions = useMemo(() => ([
        {label: t('contentBulkEdit.allTypes'), value: ''},
        ...contentTypes
    ]), [contentTypes, t]);

    const authorOptions = useMemo(() => ([
        {label: t('contentBulkEdit.allAuthors'), value: ''},
        ...authors
    ]), [authors, t]);

    const languageOptions = useMemo(() => {
        return (languages || []).map(language => ({
            label: language,
            value: language
        }));
    }, [languages]);

    const handleOpenPathPicker = () => {
        const initialPath = filters.path || `/sites/${siteKey}`;

        window.CE_API.openPicker({
            type: 'editorial',
            initialSelectedItem: [initialPath],
            site: window.jahiaGWTParameters?.siteKey || siteKey,
            lang: window.jahiaGWTParameters?.uilang || selectedLanguage,
            isMultiple: false,
            setValue: ([selected]) => {
                if (selected?.path) {
                    onFilterChange('path', selected.path);
                }
            }
        });
    };

    const clearDateBlock = (fromKey, toKey) => {
        onFilterChange(fromKey, '');
        onFilterChange(toKey, '');
    };

    const getFieldClassName = fieldName => validationErrors?.[fieldName] ? `${styles.field} ${styles.fieldError}` : styles.field;

    return (
        <div className={styles.searchCriteriaLayout}>
            <Paper className={`${styles.panel} ${styles.searchPanel}`}>
                <div className={styles.panelHeader}>
                    <Typography variant="heading" weight="bold">
                        {t('contentBulkEdit.filtersTitle')}
                    </Typography>
                    <Typography variant="body">
                        {t('contentBulkEdit.subtitle')}
                    </Typography>
                </div>

                <div className={styles.filtersGrid}>
                    <div className={styles.fieldColumn}>
                        <div className={styles.field}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.text')}</Typography>
                            <Input
                                value={filters.text}
                                placeholder={t('contentBulkEdit.searchPlaceholder')}
                                onChange={event => onFilterChange('text', event.target.value)}
                            />
                        </div>

                        <div className={styles.field}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.path')}</Typography>
                            <div className={styles.pickerField}>
                                <div className={styles.pickerInput}>
                                    <Input
                                        value={filters.path}
                                        placeholder={t('contentBulkEdit.pathPlaceholder')}
                                        onChange={event => onFilterChange('path', event.target.value)}
                                    />
                                </div>
                                <Button label={t('contentBulkEdit.browse')} variant="ghost" onClick={handleOpenPathPicker}/>
                            </div>
                        </div>
                    </div>

                    <div className={styles.fieldColumn}>
                        <div className={getFieldClassName('contentType')}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.contentType')}</Typography>
                            <Dropdown
                                data={contentTypeOptions}
                                value={filters.contentType}
                                onChange={(event, item) => onFilterChange('contentType', item?.value || '')}
                            />
                            {validationErrors?.contentType && (
                                <Typography variant="caption" className={styles.fieldErrorMessage}>
                                    {t('contentBulkEdit.requiredField')}
                                </Typography>
                            )}
                        </div>

                        <div className={styles.field}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.language')}</Typography>
                            <Dropdown
                                data={languageOptions}
                                value={selectedLanguage}
                                onChange={(event, item) => onLanguageChange(item?.value || selectedLanguage)}
                            />
                        </div>
                    </div>

                    <div className={styles.fieldColumn}>
                        <div className={styles.field}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.publicationStatus')}</Typography>
                            <Dropdown
                                data={publicationStatuses}
                                value={filters.publicationStatus}
                                onChange={(event, item) => onFilterChange('publicationStatus', item?.value || '')}
                            />
                        </div>

                        <div className={styles.field}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.author')}</Typography>
                            <Dropdown
                                data={authorOptions}
                                value={filters.author}
                                onChange={(event, item) => onFilterChange('author', item?.value || '')}
                            />
                        </div>
                    </div>
                </div>
            </Paper>

            <Paper className={`${styles.panel} ${styles.searchPanel}`}>
                <div className={styles.panelHeader}>
                    <Typography variant="heading" weight="bold">
                        {t('contentBulkEdit.dateFiltersTitle')}
                    </Typography>
                </div>

                <div className={styles.dateColumns}>
                    <div className={styles.dateColumn}>
                        <div className={styles.dateColumnHeader}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.publicationDate')}</Typography>
                            <Button
                                label={t('contentBulkEdit.clear')}
                                variant="ghost"
                                size="small"
                                onClick={() => clearDateBlock('publicationFrom', 'publicationTo')}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.from')}</Typography>
                            <Input
                                type="date"
                                value={filters.publicationFrom}
                                onChange={event => onFilterChange('publicationFrom', event.target.value)}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.to')}</Typography>
                            <Input
                                type="date"
                                value={filters.publicationTo}
                                onChange={event => onFilterChange('publicationTo', event.target.value)}
                            />
                        </div>
                    </div>

                    <div className={styles.dateColumn}>
                        <div className={styles.dateColumnHeader}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.creationDate')}</Typography>
                            <Button
                                label={t('contentBulkEdit.clear')}
                                variant="ghost"
                                size="small"
                                onClick={() => clearDateBlock('creationFrom', 'creationTo')}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.from')}</Typography>
                            <Input
                                type="date"
                                value={filters.creationFrom}
                                onChange={event => onFilterChange('creationFrom', event.target.value)}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.to')}</Typography>
                            <Input
                                type="date"
                                value={filters.creationTo}
                                onChange={event => onFilterChange('creationTo', event.target.value)}
                            />
                        </div>
                    </div>

                    <div className={styles.dateColumn}>
                        <div className={styles.dateColumnHeader}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.filters.lastModificationDate')}</Typography>
                            <Button
                                label={t('contentBulkEdit.clear')}
                                variant="ghost"
                                size="small"
                                onClick={() => clearDateBlock('modificationFrom', 'modificationTo')}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.from')}</Typography>
                            <Input
                                type="date"
                                value={filters.modificationFrom}
                                onChange={event => onFilterChange('modificationFrom', event.target.value)}
                            />
                        </div>
                        <div className={styles.field}>
                            <Typography variant="caption">{t('contentBulkEdit.filters.to')}</Typography>
                            <Input
                                type="date"
                                value={filters.modificationTo}
                                onChange={event => onFilterChange('modificationTo', event.target.value)}
                            />
                        </div>
                    </div>
                </div>
            </Paper>
        </div>
    );
};

SearchFilters.propTypes = {
    t: PropTypes.func.isRequired,
    filters: PropTypes.object.isRequired,
    validationErrors: PropTypes.object.isRequired,
    contentTypes: PropTypes.array.isRequired,
    authors: PropTypes.array.isRequired,
    languages: PropTypes.array.isRequired,
    selectedLanguage: PropTypes.string.isRequired,
    siteKey: PropTypes.string.isRequired,
    onFilterChange: PropTypes.func.isRequired,
    onLanguageChange: PropTypes.func.isRequired
};
