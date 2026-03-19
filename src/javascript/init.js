import {registry} from '@jahia/ui-extender';
import register from './AdminPanel.register';

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
                    icon: window.jahia.moonstone.toIconComponent('<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h16v3H4V6zm0 5h16v3H4v-3zm0 5h10v3H4v-3z"/></svg>'),
                    label: 'contentBulkEdit:accordion.title',
                    appsTarget: 'contentToolsAccordionApps'
                });
            }

            await register();
        }
    });
}
