import React, { Component } from 'react'
import { Link } from 'react-router'
import { connect } from "react-redux";

import MetadataHeader from '../components/database/MetadataHeader.jsx';

import {
    getDatabases,
    getDatabaseIdfields,
    getEditingDatabaseWithTableMetadataStrengths,
    getEditingTable
} from "../selectors";
import * as metadataActions from "../datamodel";

const mapDispatchToProps = {
    ...metadataActions,
}

const mapStateToProps = (state, props) => {
    return {
        databaseId:           parseInt(props.params.databaseId),
        tableId:              parseInt(props.params.tableId),
        databases:            getDatabases(state, props),
        idfields:             getDatabaseIdfields(state, props),
        databaseMetadata:     getEditingDatabaseWithTableMetadataStrengths(state, props),
        editingTable:         getEditingTable(state, props)
    }
}

@connect(mapStateToProps, mapDispatchToProps)
class MetadataWrapperApp extends Component {
    state = {
        isShowingSchema: false
    }

    render () {
        return (
            <div className="wrapper">
                <div className="mt1">
                    <MetadataHeader
                        ref="header"
                        databaseId={this.props.databaseMetadata ? this.props.databaseMetadata.id : null}
                        databases={this.props.databases}
                        selectDatabase={this.props.selectDatabase}
                        isShowingSchema={this.state.isShowingSchema}
                        toggleShowSchema={this.toggleShowSchema}
                    />
                </div>
                <ol className="flex align-center border-bottom py1">
                    <li>
                        <Link
                            className="inline-block bordered border-dark rounded p2"
                            activeClassName="bg-brand text-white border-brand"
                            to="/admin/datamodel/database/metrics"
                        >
                            Metrics
                        </Link>
                    </li>
                    <li>
                        <Link
                            className="inline-block bordered border-dark rounded p2"
                            activeClassName="bg-brand text-white border-brand"
                            to="/admin/datamodel/database"
                        >
                            Segments and tables
                        </Link>
                    </li>
                    <li>
                        <Link
                            className="inline-block bordered border-dark rounded p2"
                            activeClassName="bg-brand text-white border-brand"
                            to="/admin/datamodel/database/dimensions"
                        >
                            Dimensions
                        </Link>
                    </li>
                </ol>
                { this.props.children }
            </div>
        )
    }
}

export default MetadataWrapperApp
