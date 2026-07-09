import React, {useCallback, useMemo, useState} from 'react';
import PropTypes from 'prop-types';
import {
    Button,
    ChevronDown,
    ChevronUp,
    Checkbox,
    Table,
    TableBody,
    TableBodyCell,
    TableHead,
    TableHeadCell,
    TableRow,
    Typography
} from '@jahia/moonstone';
import dayjs from 'dayjs';
import styles from '../BulkEdit.module.scss';

const formatDate = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '';

// Extracts the sortable string value of a node for a given column key
const getSortValue = (node, key) => {
    if (key === 'name') {
        return node.displayName || node.name || '';
    }

    if (key === 'status') {
        return node.publicationStatus || '';
    }

    if (key === 'tags') {
        return (node.tags || []).join(', ');
    }

    if (key === 'categories') {
        return (node.categories || []).join(', ');
    }

    if (key.startsWith('prop:')) {
        return getPropertyValue(node, key.slice(5));
    }

    return '';
};

const STATUS_DOT_CLASS = {
    published: 'statusDotPublished',
    modified: 'statusDotModified',
    notPublished: 'statusDotNotPublished',
    unpublished: 'statusDotUnpublished',
    markedForDeletion: 'statusDotMarkedForDeletion'
};

const PublicationStatus = ({t, status}) => (
    <span className={styles.statusCell}>
        <span className={`${styles.statusDot} ${styles[STATUS_DOT_CLASS[status] || 'statusDotNotPublished']}`}/>
        <Typography variant="caption">{t(`contentBulkEdit.${status || 'notPublished'}`)}</Typography>
    </span>
);

PublicationStatus.propTypes = {
    t: PropTypes.func.isRequired,
    status: PropTypes.string
};

const getPropertyValue = (node, propertyName) => {
    const property = (node.propertyValues || []).find(item => item.name === propertyName);
    return property?.value || '';
};

export const ResultsTable = ({
    t,
    nodes,
    selectedProperties,
    propertyLabels,
    selectedNodes,
    onToggleAll,
    onToggleRow
}) => {
    const [expandedNodeUuid, setExpandedNodeUuid] = useState(null);
    const [sortConfig, setSortConfig] = useState(null);
    const allSelected = nodes.length > 0 && selectedNodes.length === nodes.length;

    const orderedColumns = useMemo(() => {
        return selectedProperties.map(name => ({
            name,
            label: propertyLabels[name] || name
        }));
    }, [selectedProperties, propertyLabels]);

    const toggleExpand = useCallback(uuid => {
        setExpandedNodeUuid(current => current === uuid ? null : uuid);
    }, []);

    const handleSort = useCallback(key => {
        setSortConfig(current => {
            if (current?.key !== key) {
                return {key, direction: 'asc'};
            }

            return current.direction === 'asc' ? {key, direction: 'desc'} : null;
        });
    }, []);

    const sortedNodes = useMemo(() => {
        if (!sortConfig) {
            return nodes;
        }

        const factor = sortConfig.direction === 'desc' ? -1 : 1;
        return [...nodes].sort((left, right) =>
            factor * getSortValue(left, sortConfig.key).localeCompare(getSortValue(right, sortConfig.key), undefined, {sensitivity: 'base', numeric: true})
        );
    }, [nodes, sortConfig]);

    const expandedNode = useMemo(() => {
        return nodes.find(node => node.uuid === expandedNodeUuid) || null;
    }, [expandedNodeUuid, nodes]);

    const renderSortableHeader = (key, label) => {
        const isActive = sortConfig?.key === key;
        return (
            <button
                type="button"
                className={styles.sortableHeader}
                aria-sort={isActive ? (sortConfig.direction === 'asc' ? 'ascending' : 'descending') : undefined}
                onClick={() => handleSort(key)}
            >
                {label}
                {isActive && (sortConfig.direction === 'asc' ? <ChevronUp/> : <ChevronDown/>)}
            </button>
        );
    };

    return (
        <div className={styles.tableContainer}>
            <Table aria-label={t('contentBulkEdit.resultsTitle')}>
                <TableHead>
                    <TableRow>
                        <TableHeadCell className={styles.checkboxCell}>
                            <Checkbox
                                checked={allSelected}
                                aria-label={t('contentBulkEdit.table.select')}
                                onChange={event => onToggleAll(event.target.checked)}
                            />
                        </TableHeadCell>
                        <TableHeadCell>{renderSortableHeader('name', t('contentBulkEdit.table.name'))}</TableHeadCell>
                        <TableHeadCell>{renderSortableHeader('status', t('contentBulkEdit.table.status'))}</TableHeadCell>
                        {orderedColumns.map(column => (
                            <TableHeadCell key={column.name}>{renderSortableHeader(`prop:${column.name}`, column.label)}</TableHeadCell>
                        ))}
                        <TableHeadCell>{renderSortableHeader('tags', t('contentBulkEdit.table.tags'))}</TableHeadCell>
                        <TableHeadCell>{renderSortableHeader('categories', t('contentBulkEdit.table.category'))}</TableHeadCell>
                        <TableHeadCell/>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {sortedNodes.map(node => {
                        const isSelected = selectedNodes.includes(node.uuid);
                        const isExpanded = expandedNodeUuid === node.uuid;

                        return (
                            <TableRow key={node.uuid} className={isSelected ? styles.selectedRow : ''}>
                                <TableBodyCell className={styles.checkboxCell}>
                                    <Checkbox
                                        checked={isSelected}
                                        aria-label={t('contentBulkEdit.table.select')}
                                        onChange={event => onToggleRow(node.uuid, event.target.checked)}
                                    />
                                </TableBodyCell>
                                <TableBodyCell>
                                    <Typography
                                        variant="body"
                                        weight="bold"
                                        className={`${styles.cellEllipsis} ${styles.nameCell}`}
                                        title={node.displayName || node.name}
                                    >
                                        {node.displayName || node.name}
                                    </Typography>
                                </TableBodyCell>
                                <TableBodyCell>
                                    <PublicationStatus t={t} status={node.publicationStatus}/>
                                </TableBodyCell>
                                {orderedColumns.map(column => {
                                    const propertyValue = getPropertyValue(node, column.name);
                                    return (
                                        <TableBodyCell key={`${node.uuid}-${column.name}`}>
                                            <Typography variant="caption" className={styles.cellEllipsis} title={propertyValue}>
                                                {propertyValue}
                                            </Typography>
                                        </TableBodyCell>
                                    );
                                })}
                                <TableBodyCell>
                                    <Typography variant="caption" className={styles.cellEllipsis} title={(node.tags || []).join(', ')}>
                                        {(node.tags || []).join(', ')}
                                    </Typography>
                                </TableBodyCell>
                                <TableBodyCell>
                                    <Typography variant="caption" className={styles.cellEllipsis} title={(node.categories || []).join(', ')}>
                                        {(node.categories || []).join(', ')}
                                    </Typography>
                                </TableBodyCell>
                                <TableBodyCell>
                                    <Button
                                        icon={isExpanded ? <ChevronUp/> : <ChevronDown/>}
                                        variant="ghost"
                                        size="small"
                                        onClick={() => toggleExpand(node.uuid)}
                                    />
                                </TableBodyCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>

            {expandedNode && (
                <div className={styles.expandedBlock}>
                    <div className={styles.expandedBlockHeader}>
                        <Typography variant="subheading" weight="bold">
                            {expandedNode.displayName || expandedNode.name}
                        </Typography>
                        <Typography variant="caption">
                            {expandedNode.nodeType}
                        </Typography>
                    </div>

                    <div className={styles.metadataGrid}>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.path')}</Typography>
                            <Typography variant="body">{expandedNode.path}</Typography>
                        </div>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.status')}</Typography>
                            <Typography variant="body">{expandedNode.publicationStatus}</Typography>
                        </div>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.publicationDate')}</Typography>
                            <Typography variant="body">{formatDate(expandedNode.publicationDate)}</Typography>
                        </div>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.created')}</Typography>
                            <Typography variant="body">{formatDate(expandedNode.created)}</Typography>
                        </div>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.modified')}</Typography>
                            <Typography variant="body">{formatDate(expandedNode.lastModified)}</Typography>
                        </div>
                        <div className={styles.metadataItem}>
                            <Typography variant="caption" weight="bold">{t('contentBulkEdit.table.author')}</Typography>
                            <Typography variant="body">{expandedNode.author || '-'}</Typography>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

ResultsTable.propTypes = {
    t: PropTypes.func.isRequired,
    nodes: PropTypes.array.isRequired,
    selectedProperties: PropTypes.array.isRequired,
    propertyLabels: PropTypes.object.isRequired,
    selectedNodes: PropTypes.array.isRequired,
    onToggleAll: PropTypes.func.isRequired,
    onToggleRow: PropTypes.func.isRequired
};
