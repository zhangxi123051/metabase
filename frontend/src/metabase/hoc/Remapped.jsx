import React, { Component } from "react";
import { connect } from "react-redux";

import { createSelector } from "reselect";

import { getMetadata } from "metabase/selectors/metadata";
import { fetchRemapping } from "metabase/redux/metadata";

const getFieldFromColumnProp = createSelector(
    [getMetadata, (state, props) => props.column && props.column.id],
    (metadata, columnId) => metadata.fields[columnId]
);

const getFieldFromDisplayColumnProp = createSelector(
    [getMetadata, (state, props) => props.displayColumn && props.displayColumn.id],
    (metadata, displayColumnId) => metadata.fields[displayColumnId]
);

const getDisplayValue = createSelector(
    [(state, props) => props.value, getFieldFromColumnProp],
    (value, field) => field && field.remappedValue(value)
);

const getDisplayColumn = createSelector(
    [getFieldFromColumnProp, getFieldFromDisplayColumnProp],
    (field, displayField) => displayField || (field && field.remappedField())
);

const mapStateToProps = (state, props) => ({
    displayValue: getDisplayValue(state, props),
    displayColumn: getDisplayColumn(state, props)
});

const mapDispatchToProps = {
    fetchRemapping
};

export default ComposedComponent =>
    @connect(mapStateToProps, mapDispatchToProps)
    class extends Component {
        static displayName = "Remapped[" +
            (ComposedComponent.displayName || ComposedComponent.name) +
            "]";

        componentWillMount() {
            this.fetchRemapping(this.props);
        }
        componentWillReceiveProps(nextProps) {
            if (
                (this.props.value !== nextProps.value ||
                    this.props.column !== nextProps.column)
            ) {
                this.fetchRemapping(nextProps);
            }
        }
        fetchRemapping(props) {
            if (props.column && props.displayColumn) {
              props.fetchRemapping(props.value, props.column.id, props.displayColumn.id);
            }
        }

        render() {
            let { displayValue, displayColumn } = this.props;
            if (displayValue === undefined) {
                displayColumn = null;
            }
            return (
                <ComposedComponent
                    {...this.props}
                    displayValue={displayValue}
                    displayColumn={displayColumn}
                />
            );
        }
    };
