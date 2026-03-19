import React from 'react';
import {registry} from '@jahia/ui-extender';
import register from './AdminPanel.register';
import BulkEditIcon from './icons/BulkEditIcon';

export default function () {
    registry.add('callback', 'contentBulkEdit', {
        targets: ['jahiaApp-init:99'],
        callback: async () => {
            window.jahia.i18n.loadNamespaces('contentBulkEdit');

            const accordionType = 'accordionItem';
            const accordionKey = 'contentToolsAccordion';
            const accordionExists = window.jahia.uiExtender.registry.get(accordionType, accordionKey);

            if (!accordionExists) {
                registry.add(accordionType, accordionKey, registry.get(accordionType, 'renderDefaultApps'), {
                    targets: ['jcontent:75'],
                    icon: <BulkEditIcon/>,
                    label: 'contentBulkEdit:accordion.title',
                    appsTarget: 'contentToolsAccordionApps'
                });
            }

            await register();
        }
    });
}
