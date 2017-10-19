import React from 'react'
import InsightCard from 'metabase/xray/components/InsightCard'

export const component = InsightCard

export const description = `
    A card used to disply a data insight.
`

export const examples = {
    'Default': (
        <InsightCard
            title='Stationary data'
            text='Yo this data is very stationary doncha know.'
            term='stationary'
            termDefinition='Stationary data occurs when turtles are in your data center'
            autoSize={false}
        />
    ),
    'Multiple cards': (
        <div className="Grid Grid--gutters Grid--1of4">
            <div className="Grid-cell">
                <InsightCard
                    title='Stationary data'
                    text='Yo this data is very stationary doncha know.'
                    term='stationary'
                    termDefinition='Stationary data occurs when turtles are in your data center.'
                />
            </div>
            <div className="Grid-cell">
                <InsightCard
                    title='Seasonal'
                    text='This data appears to have seasonality'
                    term='seasonality'
                    termDefinition='Seasonality is when you have to get that special lamp otherwise your data gets sad.'
                />
            </div>
        </div>
    )
}

