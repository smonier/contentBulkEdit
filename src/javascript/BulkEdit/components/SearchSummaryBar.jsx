import React from 'react';
import PropTypes from 'prop-types';
import {Button, Paper, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

const EXTRA_FILTER_NAMES = [
    'publicationStatus',
    'publicationFrom',
    'publicationTo',
    'creationFrom',
    'creationTo',
    'modificationFrom',
    'modificationTo'
];

export const SearchSummaryBar = ({t, filters, typeLabel, language, resultsCount, selectedCount, onEdit}) => {
    const activeExtraFilters = EXTRA_FILTER_NAMES.filter(name => Boolean(filters[name])).length;

    return (
        <Paper className={`${styles.panel} ${styles.summaryBar}`}>
            <div className={styles.summaryInfo}>
                <Typography variant="body" weight="bold">{typeLabel}</Typography>
                <span className={styles.summaryChip}>{language}</span>
                {filters.text && <span className={styles.summaryChip} title={filters.text}>&ldquo;{filters.text}&rdquo;</span>}
                {filters.path && <span className={styles.summaryChip} title={filters.path}>{filters.path}</span>}
                {filters.author && <span className={styles.summaryChip}>{filters.author}</span>}
                {activeExtraFilters > 0 && (
                    <span className={styles.summaryChip}>
                        {t('contentBulkEdit.activeFilters', {count: activeExtraFilters})}
                    </span>
                )}
                <Typography variant="body">
                    {t('contentBulkEdit.resultsSummary', {results: resultsCount, selected: selectedCount})}
                </Typography>
            </div>
            <Button label={t('contentBulkEdit.editCriteria')} variant="outlined" onClick={onEdit}/>
        </Paper>
    );
};

SearchSummaryBar.propTypes = {
    t: PropTypes.func.isRequired,
    filters: PropTypes.object.isRequired,
    typeLabel: PropTypes.string,
    language: PropTypes.string.isRequired,
    resultsCount: PropTypes.number.isRequired,
    selectedCount: PropTypes.number.isRequired,
    onEdit: PropTypes.func.isRequired
};
