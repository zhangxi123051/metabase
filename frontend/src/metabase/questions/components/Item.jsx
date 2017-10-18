/* @flow */
import React from "react";
import { Link } from "react-router";
import cx from "classnames";
import pure from "recompose/pure";

import CheckBox from "metabase/components/CheckBox";
import Icon from "metabase/components/Icon";
import ModalWithTrigger from "metabase/components/ModalWithTrigger";
import Tooltip from "metabase/components/Tooltip";

import CollectionBadge from "./CollectionBadge";
import Labels from "./Labels";
import MoveToCollection from "../containers/MoveToCollection";

import * as Urls from "metabase/lib/urls";

const ITEM_ICON_SIZE = 20;

type ItemProps = {
    entity: {},
    id: number,
    name: string,
    created: string,
    description: string,
    by: string,
    labels: [],
    collection: {},
    selected: bool,
    favorite: bool,
    archived: bool,
    icon: string,
    setItemSelected: () => void,
    setFavorited: () => void,
    setArchived: () => void,
    onEntityClick: () => void,
    showCollectionName: () => void,
};

const Item = ({
    entity,
    id,
    name,
    description,
    labels,
    created,
    by,
    favorite,
    collection,
    archived,
    icon,
    selected,
    setItemSelected,
    setFavorited,
    setArchived,
    showCollectionName,
    onEntityClick
}: ItemProps) =>
    <div className="hover-parent hover--visibility mb2 md-mb0">
        <div className="md-flex flex-full align-center">
            <div className="relative md-flex" style={{ width: ITEM_ICON_SIZE, height: ITEM_ICON_SIZE }}>
                { icon &&
                    <Icon
                        className={cx(
                            "text-light-blue absolute top left visible",
                            { "hover-child--hidden": !!setItemSelected }
                        )}
                        name={icon}
                        size={ITEM_ICON_SIZE}
                    />
                }
                { setItemSelected &&
                    <span className={cx(
                        "absolute top left hide md-show",
                        { "visible": selected },
                        { "hover-child": !selected }
                    )}>
                        <CheckBox
                            checked={selected}
                            onChange={({ target }) => setItemSelected({ [id]: target.checked })}
                            size={ITEM_ICON_SIZE}
                            padding={3}
                        />
                    </span>
                }
            </div>
            <ItemBody
                entity={entity}
                id={id}
                name={name}
                description={description}
                labels={labels}
                favorite={favorite}
                collection={showCollectionName && collection}
                setFavorited={setFavorited}
                onEntityClick={onEntityClick}
            />
        </div>
        <div className="md-flex flex-column ml-auto">
            <ItemCreated
                by={by}
                created={created}
            />
            { setArchived &&
                <div className="hover-child hide md-show mt1 ml-auto">
                    <ModalWithTrigger
                        full
                        triggerElement={
                            <Tooltip tooltip="Move to a collection">
                                <Icon
                                    className="text-light-blue cursor-pointer text-brand-hover transition-color mx2"
                                    name="move"
                                    size={18}
                                />
                            </Tooltip>
                        }
                    >
                        <MoveToCollection
                            questionId={id}
                            initialCollectionId={collection && collection.id}
                        />
                    </ModalWithTrigger>
                    <Tooltip tooltip={archived ? "Unarchive" : "Archive"}>
                        <Icon
                            className="text-light-blue cursor-pointer text-brand-hover transition-color"
                            name={ archived ? "unarchive" : "archive"}
                            onClick={() => setArchived(id, !archived, true)}
                            size={18}
                        />
                    </Tooltip>
                </div>
            }
        </div>
    </div>

type ItemBodyProps = {
    entity: {},
    description: string,
    favorite: bool,
    id: number,
    name: string,
    setFavorited: () => void,
    labels: [],
    collection: {},
    onEntityClick: () => void
}

const ItemBody = pure(({
    entity,
    id,
    name,
    description,
    labels,
    favorite,
    collection,
    setFavorited,
    onEntityClick
}: ItemBodyProps) =>
    <div>
        <div className="md-flex">
            <Link to={Urls.question(id)} onClick={onEntityClick && ((e) => { e.preventDefault(); onEntityClick(entity); })}>
                <h2>{name}</h2>
            </Link>
            { collection &&
                <CollectionBadge collection={collection} />
            }
            { favorite != null && setFavorited &&
                <Tooltip tooltip={favorite ? "Unfavorite" : "Favorite"}>
                    <Icon
                        className={cx(
                            "md-flex cursor-pointer hide md-show",
                            {"hover-child text-light-blue text-brand-hover": !favorite},
                            {"visible text-gold": favorite}
                        )}
                        name={favorite ? "star" : "staroutline"}
                        size={ITEM_ICON_SIZE}
                        onClick={() => setFavorited(id, !favorite) }
                    />
                </Tooltip>
            }
            <Labels labels={labels} />
        </div>
        <div className={cx({ 'text-slate': description }, { 'text-light-blue': !description })}>
            {description ? description : "No description yet"}
        </div>
    </div>
);

type ItemCreatedProps = {
    created: string,
    by: string
}

const ItemCreated = pure(({ created, by }: ItemCreatedProps) =>
    (created || by) ?
        <div>
            {"Created" + (created ? ` ${created}` : ``) + (by ? ` by ${by}` : ``)}
        </div>
    :
        null
);

export default pure(Item);
