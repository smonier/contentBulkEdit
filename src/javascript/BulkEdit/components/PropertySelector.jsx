import React, {useMemo} from 'react';
import PropTypes from 'prop-types';
import {Paper, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

// Groups consecutive properties sharing the same declaring node type, mirroring
// content editor's fieldsets (own type first, then one section per mixin).
const buildSections = properties => {
    const sections = [];
    properties.forEach(property => {
        const key = property.declaringNodeType || '';
        const lastSection = sections[sections.length - 1];
        if (lastSection && lastSection.key === key) {
            lastSection.items.push(property);
        } else {
            sections.push({
                key,
                label: property.declaringNodeTypeLabel || key,
                items: [property]
            });
        }
    });
    return sections;
};

export const PropertySelector = ({t, isLoading, properties, selectedProperties, onToggleProperty}) => {
    const sections = useMemo(() => buildSections(properties), [properties]);

    return (
        <Paper className={styles.panel}>
            <div className={styles.panelHeader}>
                <Typography variant="heading" weight="bold">
                    {t('contentBulkEdit.propertiesTitle')}
                </Typography>
                <Typography variant="body">
                    {t('contentBulkEdit.noProperties')}
                </Typography>
            </div>

            {isLoading && (
                <Typography variant="body">{t('contentBulkEdit.loading')}</Typography>
            )}

            {!isLoading && (
                <div className={styles.propertySections}>
                    {sections.map(section => (
                        <div key={section.key} className={styles.propertySection}>
                            {sections.length > 1 && (
                                <Typography variant="caption" weight="bold" title={section.key}>
                                    {section.label}
                                </Typography>
                            )}
                            <div className={styles.propertyChips}>
                                {section.items.map(property => {
                                    const isSelected = selectedProperties.includes(property.name);
                                    return (
                                        <button
                                            key={property.name}
                                            type="button"
                                            className={`${styles.propertyChip} ${isSelected ? styles.propertyChipSelected : ''}`}
                                            title={property.name}
                                            aria-pressed={isSelected}
                                            onClick={() => onToggleProperty(property.name, !isSelected)}
                                        >
                                            {property.label}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </Paper>
    );
};

PropertySelector.propTypes = {
    t: PropTypes.func.isRequired,
    isLoading: PropTypes.bool,
    properties: PropTypes.array.isRequired,
    selectedProperties: PropTypes.array.isRequired,
    onToggleProperty: PropTypes.func.isRequired
};
