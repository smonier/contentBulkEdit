import React, {useCallback, useMemo} from 'react';
import PropTypes from 'prop-types';
import {Dropdown, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

const buildTree = (categories, parentIdentifier = null) => {
    return categories
        .filter(category => (category.parentIdentifier || null) === parentIdentifier)
        .sort((left, right) => left.label.localeCompare(right.label))
        .map(category => {
            const children = buildTree(categories, category.identifier);

            return {
                id: category.identifier,
                value: category.identifier,
                label: category.label,
                hasChildren: children.length > 0,
                isClosable: children.length > 0,
                children: children.length > 0 ? children : undefined
            };
        });
};

export const CategoryTreeInput = ({t, categories, selectedIds, onChange}) => {
    const categoriesById = useMemo(() => {
        return categories.reduce((acc, category) => {
            acc[category.identifier] = category;
            return acc;
        }, {});
    }, [categories]);

    const treeData = useMemo(() => buildTree(categories), [categories]);

    const updateSelection = useCallback(nextIds => {
        const nextLabels = nextIds
            .map(identifier => categoriesById[identifier]?.label)
            .filter(Boolean);

        onChange(nextIds, nextLabels);
    }, [categoriesById, onChange]);

    const handleChange = useCallback((event, item) => {
        if (!item?.value) {
            return;
        }

        const nextIds = selectedIds.includes(item.value) ?
            selectedIds.filter(identifier => identifier !== item.value) :
            [...selectedIds, item.value];

        updateSelection(nextIds);
    }, [selectedIds, updateSelection]);

    const handleClear = useCallback(() => {
        updateSelection([]);
    }, [updateSelection]);

    if (treeData.length === 0) {
        return (
            <div className={styles.categoryDropdown}>
                <Typography variant="caption">{t('contentBulkEdit.table.noCategory')}</Typography>
            </div>
        );
    }

    return (
        <Dropdown
            hasSearch
            className={styles.categoryDropdown}
            placeholder={t('contentBulkEdit.categoryPlaceholder')}
            treeData={treeData}
            values={selectedIds}
            searchEmptyText={t('contentBulkEdit.categorySearchEmpty')}
            onChange={handleChange}
            onClear={selectedIds.length > 0 ? handleClear : undefined}
        />
    );
};

CategoryTreeInput.propTypes = {
    t: PropTypes.func.isRequired,
    categories: PropTypes.array.isRequired,
    selectedIds: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
};
