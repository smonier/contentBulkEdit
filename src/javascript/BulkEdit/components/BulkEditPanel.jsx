import React from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, Dropdown, Input, Paper, Typography} from '@jahia/moonstone';
import {BulkFieldInput} from './BulkFieldInput';
import {CategoryTreeInput} from './CategoryTreeInput';
import styles from '../BulkEdit.module.scss';

export const BulkEditPanel = ({
    t,
    siteKey,
    selectedProperties,
    propertyLabels,
    propertyDefinitions,
    selectedLanguage,
    bulkValues,
    clearFlags,
    propertyModes,
    tagMode,
    categoryMode,
    categoryData,
    selectedCategoryIds,
    selectedCategoryLabels,
    selectedRowsCount,
    changesCount,
    isApplyEnabled,
    onBulkValueChange,
    onToggleClear,
    onPropertyModeChange,
    onTagModeChange,
    onCategoryModeChange,
    onCategoryChange,
    onApply
}) => {
    const modeOptions = [
        {label: t('contentBulkEdit.modeReplace'), value: 'replace'},
        {label: t('contentBulkEdit.modeAppend'), value: 'append'}
    ];

    return (
        <Paper className={`${styles.panel} ${styles.bulkEditPanel}`}>
            <div className={styles.panelHeader}>
                <Typography variant="heading" weight="bold">
                    {t('contentBulkEdit.bulkPanelTitle')}
                </Typography>
                <Typography variant="body">
                    {t('contentBulkEdit.bulkPanelSubtitle', {count: selectedRowsCount})}
                </Typography>
            </div>

            <div className={styles.bulkEditPanelContent}>
                {selectedProperties.length === 0 && (
                    <Typography variant="body">{t('contentBulkEdit.bulkPanelEmpty')}</Typography>
                )}

                {selectedProperties.map(propertyName => {
                    const isCleared = Boolean(clearFlags[propertyName]);
                    const isMultiple = Boolean(propertyDefinitions[propertyName]?.multiple);
                    return (
                        <div key={propertyName} className={styles.field}>
                            <div className={styles.fieldLabelRow}>
                                <Typography variant="caption" weight="bold">
                                    {propertyLabels[propertyName] || propertyName}
                                </Typography>
                                {propertyDefinitions[propertyName]?.i18n && (
                                    <Typography variant="caption" className={styles.languageTag}>
                                        {t('contentBulkEdit.bulkPanelLanguage', {lang: selectedLanguage})}
                                    </Typography>
                                )}
                            </div>
                            <label className={styles.clearRow}>
                                <Checkbox
                                    checked={isCleared}
                                    onChange={event => onToggleClear(propertyName, event.target.checked)}
                                />
                                <Typography variant="caption">{t('contentBulkEdit.clearValue')}</Typography>
                            </label>
                            {!isCleared && isMultiple && (
                                <Dropdown
                                    data={modeOptions}
                                    value={propertyModes[propertyName] || 'replace'}
                                    onChange={(event, item) => onPropertyModeChange(propertyName, item?.value || 'replace')}
                                />
                            )}
                            {!isCleared && (
                                <BulkFieldInput
                                    t={t}
                                    definition={propertyDefinitions[propertyName] || {name: propertyName}}
                                    value={bulkValues[propertyName]}
                                    siteKey={siteKey}
                                    language={selectedLanguage}
                                    onChange={newValue => onBulkValueChange(propertyName, newValue)}
                                />
                            )}
                        </div>
                    );
                })}

                <div className={styles.field}>
                    <div className={styles.fieldLabelRow}>
                        <Typography variant="caption" weight="bold">
                            {t('contentBulkEdit.table.tags')}
                        </Typography>
                    </div>
                    <Dropdown
                        data={modeOptions}
                        value={tagMode}
                        onChange={(event, item) => onTagModeChange(item?.value || 'replace')}
                    />
                    <Input
                        value={bulkValues.tags || ''}
                        placeholder={t('contentBulkEdit.tagsPlaceholder')}
                        onChange={event => onBulkValueChange('tags', event.target.value)}
                    />
                </div>

                <div className={styles.field}>
                    <div className={styles.fieldLabelRow}>
                        <Typography variant="caption" weight="bold">
                            {t('contentBulkEdit.table.category')}
                        </Typography>
                    </div>
                    <Dropdown
                        data={modeOptions}
                        value={categoryMode}
                        onChange={(event, item) => onCategoryModeChange(item?.value || 'replace')}
                    />
                    <CategoryTreeInput
                        t={t}
                        categories={categoryData}
                        selectedIds={selectedCategoryIds}
                        onChange={onCategoryChange}
                    />
                    {selectedCategoryLabels.length > 0 && (
                        <Typography variant="caption">
                            {selectedCategoryLabels.join(', ')}
                        </Typography>
                    )}
                </div>

                <div className={styles.panelFooter}>
                    <Typography variant="caption">
                        {t('contentBulkEdit.panelRecap', {rows: selectedRowsCount, changes: changesCount})}
                    </Typography>
                    <div title={isApplyEnabled ? undefined : t('contentBulkEdit.applyHint')}>
                        <Button
                            label={t('contentBulkEdit.apply')}
                            color="accent"
                            isDisabled={!isApplyEnabled}
                            onClick={onApply}
                        />
                    </div>
                </div>
            </div>
        </Paper>
    );
};

BulkEditPanel.propTypes = {
    t: PropTypes.func.isRequired,
    siteKey: PropTypes.string.isRequired,
    selectedProperties: PropTypes.array.isRequired,
    propertyLabels: PropTypes.object.isRequired,
    propertyDefinitions: PropTypes.object.isRequired,
    selectedLanguage: PropTypes.string.isRequired,
    bulkValues: PropTypes.object.isRequired,
    clearFlags: PropTypes.object.isRequired,
    propertyModes: PropTypes.object.isRequired,
    tagMode: PropTypes.string.isRequired,
    categoryMode: PropTypes.string.isRequired,
    categoryData: PropTypes.array.isRequired,
    selectedCategoryIds: PropTypes.array.isRequired,
    selectedCategoryLabels: PropTypes.array.isRequired,
    selectedRowsCount: PropTypes.number.isRequired,
    changesCount: PropTypes.number.isRequired,
    isApplyEnabled: PropTypes.bool.isRequired,
    onBulkValueChange: PropTypes.func.isRequired,
    onToggleClear: PropTypes.func.isRequired,
    onPropertyModeChange: PropTypes.func.isRequired,
    onTagModeChange: PropTypes.func.isRequired,
    onCategoryModeChange: PropTypes.func.isRequired,
    onCategoryChange: PropTypes.func.isRequired,
    onApply: PropTypes.func.isRequired
};
