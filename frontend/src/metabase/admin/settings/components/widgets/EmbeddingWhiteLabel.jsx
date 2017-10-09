import React, { Component } from 'react';

import Button from "metabase/components/Button";
import Input from "metabase/components/Input";

class EmbeddingWhiteLabel extends Component {
    constructor(props) {
        super(props)
        this.state = {
            // if a token has been redeemed then we shouldn't show the propaganda
            showToken: props.token
        }
    }

    showToken = () => {
        this.setState({ showToken: true })
    }

    render() {
        const { token } = this.props
        const { showToken } = this.state
        return (
            <div className="bordered rounded shadowed p2 full text-measure">
                { showToken
                        ? (
                            <WhiteLabelToken
                                verifyAction={() =>
                                    alert('This should go check that the token is valid')
                                }
                                token={token}
                            />
                        )
                        : (
                            <WhiteLabelPitch onClick={this.showToken} />
                        )
                }
            </div>
        )
    }
}

const WhiteLabelToken = ({ verifyAction, token }) => {
    return (
        <div className="my3 text-centered">
            <h3 className="mb3">
                { token
                    ? "Your token"
                    : "Add your token"
                }
            </h3>
            <Input
                className="input full text-centered block mr-auto ml-auto mb2"
                type="text"
                value={token}
                placeholder="xxxx-xxxx-xxxx-xxxx-xxxx"
                autofocus
            />
            <Button onClick={verifyAction}>
                { token
                    ? "Checkh token"
                    : "Verify token"
                }
            </Button>
        </div>
    )
}

const WhiteLabelPitch = ({ onClick }) => {
    return (
        <div className="text-centered my3">
            <h3>Embedding tag</h3>
            <p className="text-paragraph">Embed Metabase dashboards and questions in your app without Metabase branding.</p>
            <a className="Button Button--primary" target="_blank" href="https://store.metabase.com/products/embedding">Learn more about Whitelabel Embedding</a>
            <a className="mt3 block link" onClick={onClick}>I have a token</a>
        </div>
    )
}

export default EmbeddingWhiteLabel;
