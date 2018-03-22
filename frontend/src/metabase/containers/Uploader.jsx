import React from "react";
import { connect } from "react-redux";
import { push } from "react-router-redux";

import S from "./Uploader.css";

@connect(null, { push })
export default class Uploader extends React.Component {
  componentWillMount() {
    document.body.addEventListener("dragover", this.handleDragOver, true);
    document.body.addEventListener("dragleave", this.handleDragLeave, true);
    document.body.addEventListener("dragend", this.handleDragLeave, true);
    document.body.addEventListener("drop", this.handleDrop, true);
  }
  componentWillUnmount() {
    document.body.removeEventListener("dragover", this.handleDragOver, true);
    document.body.removeEventListener("dragleave", this.handleDragLeave, true);
    document.body.removeEventListener("dragend", this.handleDragLeave, true);
    document.body.removeEventListener("drop", this.handleDrop, true);
  }

  handleDragOver = e => {
    e.preventDefault();
    document.body.className = S.drophover;
    return false;
  };

  handleDragLeave = e => {
    e.preventDefault();
    document.body.className = "";
    return false;
  };

  handleDrop = e => {
    const { push } = this.props;

    e.preventDefault();

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/upload/upload_table");
    xhr.onload = function() {
      document.body.className = "";
      let result = JSON.parse(xhr.responseText);
      if (result.status === "ok") {
        const url = `/auto/dashboard/table/${result.table_id}`;
        push(url);
      } else {
        alert(result);
      }
    };
    xhr.send(e.dataTransfer.files[0]);
  };

  render() {
    return <span />;
  }
}
