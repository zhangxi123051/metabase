import React from "react";
import { connect } from "react-redux";

import { addUndo, createUndo } from "metabase/redux/undo";

import Toast from "metabase/components/Toast";

const mapDispatchToProps = {
  addUndo,
  createUndo,
};

const withToastTrigger = ComposedComponent =>
  @connect(() => ({}), mapDispatchToProps)
  class extends React.Component {
    static displayName = "Trigger the thing";

    _triggerToast() {
      this.props.addUndo(
        this.props.createUndo({
          type: "",
          message: <ComposedComponent />,
        }),
      );
    }

    render() {
      return <div onClick={() => this._triggerToast()}>Click me!</div>;
    }
  };

export const component = Toast;

export const description = `
A toast is an unobtrusive notification that appears after certain actions. In Metabase toasts appear in the bottom
corner of the window (left by default, but possibly right depending on RTL contexts)
`;

export const examples = {
  Default: <Toast message="A thing just happened!" />,
};
