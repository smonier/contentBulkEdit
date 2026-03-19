import {registry} from '@jahia/ui-extender';
import {DEFAULT_ROUTE} from './AdminPanel.constants';
import {AdminPanel} from './AdminPanel';
import React, {Suspense} from 'react';
import BulkEditIcon from '../icons/BulkEditIcon';

export const registerRoutes = () => {
    registry.add('adminRoute', 'contentBulkEdit', {
        targets: ['contentToolsAccordionApps'],
        icon: <BulkEditIcon/>,
        label: 'contentBulkEdit:contentBulkEdit.label',
        path: `${DEFAULT_ROUTE}*`,
        defaultPath: DEFAULT_ROUTE,
        isSelectable: true,
        requireModuleInstalledOnSite: 'contentBulkEdit',
        render: v => <Suspense fallback="loading ..."><AdminPanel match={v.match}/></Suspense>
    });

    console.debug('%c contentBulkEdit is activated', 'color: #3c8cba');
};
