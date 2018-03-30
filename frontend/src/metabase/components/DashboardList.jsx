import React from 'react'
import { Box } from 'rebass'
import { Link } from 'react-router'

import { DashboardApi } from 'metabase/services'

class DashboardLoader extends React.Component {
  state = {
    dashbaords: null,
    loading: false,
    error: null
  }

  componentWillMount () {
    this._loadDashboards()
  }

  async _loadDashboards () {
    try {
      this.setState({ loading: true })

      const dashboards = await DashboardApi.list()

      this.setState({ dashboards, loading: false })
    } catch (error) {
      console.log('error?', error)
      this.setState({ loading: false, error })
    }
  }

  render () {
    const { dashboards, error, loading } = this.state
    return this.props.children({ dashboards, error, loading })
  }
}

class DashboardList extends React.Component {
  render () {
    const { limit } = this.props
    return (
      <Box>
        <DashboardLoader>
          {({ dashboards, error, loading }) => {
            if(loading) {
              return <Box>Loading...</Box>
            }
            if(error) {
              return <Box>Error!</Box>
            }

            let dashboardList = dashboards

            if(limit) {
              dashboardList = dashboardList.slice(0, limit)
            }

            return (
              <Box>
                { dashboardList.map(dashboard => <DashboardListItem dashboard={dashboard} />) }
              </Box>
            )
          }}
        </DashboardLoader>
      </Box>
    )
  }
}

const DashboardListItem = ({ dashboard }) =>
  <Box>
    <Link to={`/dashboard/${dashboard.id}`}>
      { dashboard.name }
    </Link>
  </Box>

export default DashboardList
