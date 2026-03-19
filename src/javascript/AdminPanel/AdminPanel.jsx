import React from 'react';
import PropTypes from 'prop-types';
import {BulkEdit} from '../BulkEdit';

export const AdminPanel = ({match}) => {
    return <BulkEdit match={match}/>;
};

AdminPanel.propTypes = {
    match: PropTypes.object
};

export default AdminPanel;
