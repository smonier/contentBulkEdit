import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {Button, Typography} from '@jahia/moonstone';
import styles from '../BulkEdit.module.scss';

export const PublishDialog = ({t, isOpen, count, language, isLoading, onCancel, onConfirm}) => {
    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const handleKeyDown = event => {
            if (event.key === 'Escape') {
                onCancel();
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [isOpen, onCancel]);

    if (!isOpen) {
        return null;
    }

    return (
        <div className={styles.dialogOverlay}>
            <div className={styles.dialog}>
                <Typography variant="heading" weight="bold">
                    {t('contentBulkEdit.publishConfirmTitle')}
                </Typography>
                <Typography variant="body">
                    {t('contentBulkEdit.publishSummary', {count, lang: language})}
                </Typography>

                <div className={styles.actions}>
                    <Button label={t('contentBulkEdit.cancel')} variant="ghost" onClick={onCancel}/>
                    <Button
                        label={isLoading ? t('contentBulkEdit.publishing') : t('contentBulkEdit.publish')}
                        color="accent"
                        isDisabled={isLoading}
                        onClick={onConfirm}
                    />
                </div>
            </div>
        </div>
    );
};

PublishDialog.propTypes = {
    t: PropTypes.func.isRequired,
    isOpen: PropTypes.bool.isRequired,
    count: PropTypes.number.isRequired,
    language: PropTypes.string.isRequired,
    isLoading: PropTypes.bool,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired
};
