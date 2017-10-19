/* @flow */
import React, { Component } from 'react'
import cxs from 'cxs'

import Icon from 'metabase/components/Icon'
import Tooltip from 'metabase/components/Tooltip'

const genClasses = (props) => cxs({
    width: !props.autoSize && '22em',
    height: '10em',
    ' .Icon': {
        color: '#93a1ab'
    },
    ' header span': {
        fontSize: '0.85em',
    },
    ' p': {
        lineHeight: '1.4em'
    }

})

const termStyles = cxs({
    borderBottom: '1px dotted #DCE1E4'
})

type Props = {
    text: string,
    term: string,
    title: string,
    type: string,
    termDefinition: string,
    autoSize?: bool
}

const termIconLookup = {
    'seasonality': 'sun',
}

class InsightCard extends Component {

    props: Props

    static defaultProps = {
        autoSize: true
    }

    // TODO this should support terms with spaces
    renderTextWithAnnotation () {
        const { text, term, termDefinition } = this.props
        const position = text.indexOf(term)
        return (
            <p>
                {
                    // the text up until the term
                    text.substr(0, position)
                }
                <Tooltip tooltip={termDefinition}>
                    <span className={termStyles}>
                        {text.substr(position, term.length)}
                    </span>
                </Tooltip>
                {
                    // the rest of the text after the term
                    text.substr(position + term.length, text.length)
                }
            </p>
        )
    }
    render () {
        const { title, term } = this.props
        return (
            <div className={genClasses(this.props)}>
                <div className="bordered rounded shadowed full-height p3">
                    <header className="flex align-center">
                        <Icon name={termIconLookup[term] || 'insight'} size={24} className="mr1" />
                        <span className="text-bold text-uppercase">{title}</span>
                    </header>
                    { this.renderTextWithAnnotation() }
                </div>
            </div>
        )
    }
}

export default InsightCard
