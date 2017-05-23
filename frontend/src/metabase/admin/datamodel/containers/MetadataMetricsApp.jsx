import React, { Component } from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

import Tooltip from 'metabase/components/Tooltip'
import Icon from 'metabase/components/Icon'

const mapStateToProps = state => ({
})

import LoadingAndErrorWrapper from 'metabase/components/LoadingAndErrorWrapper'

@connect(mapStateToProps)
class MetadataMetricsApp extends Component {
    render () {
        return (
            <div className="container">
                <div className="flex py1">
                    <Link className="ml-auto" to ="/admin/datamodel/metric/create">
                        <Tooltip tooltip="Create a new metric"><Icon name="add" /></Tooltip>
                    </Link>
                </div>
                <div className="bordered border-dark rounded flex flex-full flex-column justify-center align-center full-height" style={{ minHeight: '60vh' }}>
                    <h3>You haven't created any metrics for this database yet</h3>
                </div>
            </div>
        )
    }
}

export default MetadataMetricsApp
