import React, { Component } from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

import LoadingAndErrorWrapper from 'metabase/components/LoadingAndErrorWrapper'
import Tooltip from 'metabase/components/Tooltip'
import Icon from 'metabase/components/Icon'

import insight from 'insightful'

const mapStateToProps = state => ({
    metrics: [
        { name: insight(), id: 1 },
        { name: insight(), id: 2 },
        { name: insight(), id: 3 },
        { name: insight(), id: 4 },
        { name: insight(), id: 5 },
        { name: insight(), id: 6 },
        { name: insight(), id: 7 },
        { name: insight(), id: 8 },
    ]
})

@connect(mapStateToProps)
class MetadataMetricsApp extends Component {
    render () {
        const { metrics } = this.props
        return (
            <div className="container">
                <div className="flex py1">
                    <Link className="ml-auto" to ="/admin/datamodel/metric/create">
                        <Tooltip tooltip="Create a new metric"><Icon name="add" /></Tooltip>
                    </Link>
                </div>
                <LoadingAndErrorWrapper loading={!metrics}>
                    { () =>
                        <ol className="full">
                            { metrics.map(metric =>
                                <li
                                    className="py2 border-bottom"
                                    key={metric.id}
                                >
                                    <h3>
                                        <Link
                                            className="text-brand"
                                            to={`/admin/datamodel/metric/${metric.id}`}
                                        >
                                            {metric.name}
                                        </Link>
                                    </h3>
                                </li>
                            )}
                        </ol>
                    }
                </LoadingAndErrorWrapper>
            </div>
        )
    }
}

export default MetadataMetricsApp
