import React, { Component } from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

const mapStateToProps = state => ({
})

import LoadingAndErrorWrapper from 'metabase/components/LoadingAndErrorWrapper'

@connect(mapStateToProps)
class MetadataMetricsApp extends Component {
    render () {
        return (
            <div className="container">
                <Link to ="/admin/datamodel/database/metrics/new">New metric</Link>
                <div className="bordered border-dark rounded flex flex-full flex-column justify-center align-center full-height" style={{ minHeight: '60vh' }}>
                    <h3>You haven't created any metrics for this database yet</h3>
                </div>
            </div>
        )
    }
}

export default MetadataMetricsApp
