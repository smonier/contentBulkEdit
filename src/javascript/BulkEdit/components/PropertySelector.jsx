import React from 'react';
import PropTypes from 'prop-types';
import {Checkbox, Paper, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

export const PropertySelector = ({t, isLoading, properties, selectedProperties, onToggleProperty}) => {
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

            <div className={styles.propertyList}>
                {isLoading && (
                    <Typography variant="body">{t('contentBulkEdit.loading')}</Typography>
                )}

                {!isLoading && properties.map(property => (
                    <label key={property.name} className={styles.propertyRow}>
                        <Checkbox
                            checked={selectedProperties.includes(property.name)}
                            onChange={event => onToggleProperty(property.name, event.target.checked)}
                        />
                        <div className={styles.propertyInfo}>
                            <Typography variant="body" weight="bold">
                                {property.label}
                            </Typography>
                            <Typography variant="caption">
                                {property.name}
                            </Typography>
                        </div>
                    </label>
                ))}
            </div>
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
