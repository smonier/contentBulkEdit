import React from 'react';
import PropTypes from 'prop-types';
import {useQuery} from '@apollo/client';
import {Button, Dropdown, Input, Typography} from '@jahia/moonstone';
import {GET_CATEGORIES_QUERY} from '../BulkEdit.gql-queries';
import {CategoryTreeInput} from './CategoryTreeInput';
import styles from '../BulkEdit.module.scss';

// Maps the CND picker `type` selector option to a CE_API picker type
const PICKER_TYPE_MAP = {
    image: 'image',
    file: 'file',
    page: 'page',
    editorial: 'editorial',
    editoriallink: 'editoriallink',
    folder: 'folder',
    contentfolder: 'contentfolder',
    category: 'category',
    site: 'site',
    user: 'user',
    usergroup: 'usergroup'
};

const getPickerType = definition => {
    const typeOption = (definition.selectorOptions || []).find(option => option.name === 'type');
    return (typeOption && PICKER_TYPE_MAP[typeOption.value]) || 'editorial';
};

const isReferenceType = definition => {
    const requiredType = (definition.requiredType || '').toLowerCase();
    return requiredType === 'weakreference' || requiredType === 'reference';
};

// Category-flavoured references (CND `category[...]` selector or a content-editor
// ChoiceTree override) render as a category tree scoped to the override's rootPath.
const CategoryTreeField = ({t, definition, value, siteKey, language, onChange}) => {
    const rootPath = (definition.selectorOptions || []).find(option => option.name === 'rootPath')?.value || null;
    const {data, loading} = useQuery(GET_CATEGORIES_QUERY, {
        variables: {siteKey, language, rootPath}
    });

    const categories = data?.contentBulkEdit?.getCategories || [];
    const selectedIds = value && typeof value === 'object' && value.uuid ? value.uuid.split(',') : [];

    const handleChange = (identifiers, labels) => {
        const ids = definition.multiple ? identifiers : identifiers.slice(-1);
        const idLabels = definition.multiple ? labels : labels.slice(-1);
        onChange(ids.length > 0 ? {uuid: ids.join(','), label: idLabels.join(', ')} : '');
    };

    if (loading) {
        return <Typography variant="caption">{t('contentBulkEdit.loading')}</Typography>;
    }

    return (
        <CategoryTreeInput
            t={t}
            categories={categories}
            selectedIds={selectedIds}
            onChange={handleChange}
        />
    );
};

CategoryTreeField.propTypes = {
    t: PropTypes.func.isRequired,
    definition: PropTypes.object.isRequired,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
};

const ReferencePickerInput = ({t, definition, value, siteKey, language, onChange}) => {
    const displayValue = value && typeof value === 'object' ? (value.label || value.uuid) : (value || '');
    const pickerAvailable = Boolean(window?.CE_API?.openPicker);

    const handleOpenPicker = () => {
        window.CE_API.openPicker({
            type: getPickerType(definition),
            isMultiple: Boolean(definition.multiple),
            site: window.jahiaGWTParameters?.siteKey || siteKey,
            lang: window.jahiaGWTParameters?.uilang || language,
            setValue: selection => {
                const items = (Array.isArray(selection) ? selection : [selection]).filter(Boolean);
                if (items.length === 0) {
                    return;
                }

                onChange({
                    uuid: items.map(item => item.uuid || item.id || item.path).join(','),
                    label: items.map(item => item.displayName || item.name || item.path).join(', ')
                });
            }
        });
    };

    return (
        <div className={styles.pickerField}>
            <div className={`${styles.pickerInput} ${styles.pickerValue}`}>
                <Input
                    value={displayValue}
                    placeholder={t('contentBulkEdit.pickerPlaceholder')}
                    isReadOnly={pickerAvailable}
                    onChange={event => onChange(event.target.value)}
                />
            </div>
            {displayValue !== '' && (
                <Button label={t('contentBulkEdit.clear')} variant="ghost" onClick={() => onChange('')}/>
            )}
            <Button
                label={t('contentBulkEdit.browse')}
                variant="ghost"
                isDisabled={!pickerAvailable}
                onClick={handleOpenPicker}
            />
        </div>
    );
};

ReferencePickerInput.propTypes = {
    t: PropTypes.func.isRequired,
    definition: PropTypes.object.isRequired,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
};

const getReferenceComponent = selector =>
    (['category', 'choicetree'].includes(selector) ? CategoryTreeField : ReferencePickerInput);

export const BulkFieldInput = ({t, definition, value, siteKey, language, onChange}) => {
    const selector = (definition.selectorType || '').toLowerCase();
    const requiredType = (definition.requiredType || '').toLowerCase();

    if (isReferenceType(definition)) {
        const FieldComponent = getReferenceComponent(selector);
        return (
            <FieldComponent
                t={t}
                definition={definition}
                value={value}
                siteKey={siteKey}
                language={language}
                onChange={onChange}
            />
        );
    }

    if (selector === 'choicelist' && (definition.constraints || []).length > 0) {
        const data = [
            {label: t('contentBulkEdit.selectValue'), value: ''},
            ...definition.constraints.map(constraint => ({label: constraint, value: constraint}))
        ];
        return (
            <Dropdown
                data={data}
                value={value || ''}
                onChange={(event, item) => onChange(item?.value || '')}
            />
        );
    }

    if (requiredType === 'boolean') {
        const data = [
            {label: t('contentBulkEdit.selectValue'), value: ''},
            {label: t('contentBulkEdit.yes'), value: 'true'},
            {label: t('contentBulkEdit.no'), value: 'false'}
        ];
        return (
            <Dropdown
                data={data}
                value={value || ''}
                onChange={(event, item) => onChange(item?.value || '')}
            />
        );
    }

    if (requiredType === 'date') {
        return (
            <Input
                type={selector === 'datepicker' ? 'date' : 'datetime-local'}
                value={value || ''}
                onChange={event => onChange(event.target.value)}
            />
        );
    }

    if (['long', 'double', 'decimal'].includes(requiredType)) {
        return (
            <Input
                type="number"
                step={requiredType === 'long' ? '1' : 'any'}
                value={value || ''}
                placeholder={t('contentBulkEdit.propertyPlaceholder')}
                onChange={event => onChange(event.target.value)}
            />
        );
    }

    if (['richtext', 'textarea'].includes(selector)) {
        return (
            <textarea
                className={styles.textArea}
                rows={4}
                value={value || ''}
                placeholder={t('contentBulkEdit.propertyPlaceholder')}
                onChange={event => onChange(event.target.value)}
            />
        );
    }

    return (
        <>
            <Input
                value={value || ''}
                placeholder={t('contentBulkEdit.propertyPlaceholder')}
                onChange={event => onChange(event.target.value)}
            />
            {definition.multiple && (
                <Typography variant="caption">{t('contentBulkEdit.multipleHint')}</Typography>
            )}
        </>
    );
};

BulkFieldInput.propTypes = {
    t: PropTypes.func.isRequired,
    definition: PropTypes.object.isRequired,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
};
