import React from 'react';
import PropTypes from 'prop-types';
import {Button, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

export const ConfirmDialog = ({t, isOpen, summary, onCancel, onConfirm, isLoading}) => {
    if (!isOpen) {
        return null;
    }

    return (
        <div className={styles.dialogOverlay}>
            <div className={styles.dialog}>
                <Typography variant="heading" weight="bold">
                    {t('contentBulkEdit.confirm')}
                </Typography>
                <Typography variant="body">
                    {t('contentBulkEdit.summary', {count: summary.count})}
                </Typography>

                <div className={styles.summaryBlock}>
                    <Typography variant="subheading" weight="bold">
                        {t('contentBulkEdit.summaryProperties')}
                    </Typography>
                    {summary.properties.map(item => (
                        <Typography key={item.name} variant="body">
                            {item.label}: {item.value}
                        </Typography>
                    ))}
                </div>

                {summary.tags.length > 0 && (
                    <div className={styles.summaryBlock}>
                        <Typography variant="subheading" weight="bold">
                            {t('contentBulkEdit.summaryTags')}
                        </Typography>
                        <Typography variant="body">{summary.tags.join(', ')}</Typography>
                    </div>
                )}

                {summary.categories.length > 0 && (
                    <div className={styles.summaryBlock}>
                        <Typography variant="subheading" weight="bold">
                            {t('contentBulkEdit.summaryCategories')}
                        </Typography>
                        <Typography variant="body">{summary.categories.join(', ')}</Typography>
                    </div>
                )}

                <div className={styles.actions}>
                    <Button label={t('contentBulkEdit.cancel')} variant="ghost" onClick={onCancel}/>
                    <Button
                        label={isLoading ? t('contentBulkEdit.executing') : t('contentBulkEdit.execute')}
                        color="accent"
                        isDisabled={isLoading}
                        onClick={onConfirm}
                    />
                </div>
            </div>
        </div>
    );
};

ConfirmDialog.propTypes = {
    t: PropTypes.func.isRequired,
    isOpen: PropTypes.bool.isRequired,
    summary: PropTypes.object.isRequired,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    isLoading: PropTypes.bool
};
