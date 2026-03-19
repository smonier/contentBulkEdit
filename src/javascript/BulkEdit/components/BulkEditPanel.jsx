import React from 'react';
import PropTypes from 'prop-types';
import {Input, Paper, Typography} from '@jahia/moonstone';
import {CategoryTreeInput} from './CategoryTreeInput';
import styles from '../BulkEdit.module.scss';

export const BulkEditPanel = ({
    t,
    selectedProperties,
    propertyLabels,
    bulkValues,
    categoryData,
    selectedCategoryIds,
    selectedCategoryLabels,
    selectedRowsCount,
    onBulkValueChange,
    onCategoryChange
}) => {
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

                {selectedProperties.map(propertyName => (
                    <div key={propertyName} className={styles.field}>
                        <Typography variant="caption" weight="bold">
                            {propertyLabels[propertyName] || propertyName}
                        </Typography>
                        <Input
                            value={bulkValues[propertyName] || ''}
                            placeholder={t('contentBulkEdit.propertyPlaceholder')}
                            onChange={event => onBulkValueChange(propertyName, event.target.value)}
                        />
                    </div>
                ))}

                <div className={styles.field}>
                    <Typography variant="caption" weight="bold">
                        {t('contentBulkEdit.table.tags')}
                    </Typography>
                    <Input
                        value={bulkValues.tags || ''}
                        placeholder={t('contentBulkEdit.tagsPlaceholder')}
                        onChange={event => onBulkValueChange('tags', event.target.value)}
                    />
                </div>

                <div className={styles.field}>
                    <Typography variant="caption" weight="bold">
                        {t('contentBulkEdit.table.category')}
                    </Typography>
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
            </div>
        </Paper>
    );
};

BulkEditPanel.propTypes = {
    t: PropTypes.func.isRequired,
    selectedProperties: PropTypes.array.isRequired,
    propertyLabels: PropTypes.object.isRequired,
    bulkValues: PropTypes.object.isRequired,
    categoryData: PropTypes.array.isRequired,
    selectedCategoryIds: PropTypes.array.isRequired,
    selectedCategoryLabels: PropTypes.array.isRequired,
    selectedRowsCount: PropTypes.number.isRequired,
    onBulkValueChange: PropTypes.func.isRequired,
    onCategoryChange: PropTypes.func.isRequired
};
