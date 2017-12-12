(ns metabase.mbql.resolve
  (:refer-clojure :exclude [resolve])
  (:require [clojure.set :as set]
            [metabase.mbql :as mbql]
            [metabase.models
             [field :refer [Field]]
             [table :refer [Table]]]
            [metabase.util :as u]
            [schema.core :as s]
            [toucan.db :as db])
  (:import [metabase.mbql FieldID TableID Query]))

(defprotocol Resolve
  (unresolved-field-ids [this])
  (unresolved-table-ids [this])
  (resolve-fields [this field-id->field])
  (resolve-tables [this table-id->table]))

(extend-protocol Resolve
  nil
  (unresolved-field-ids [_] nil)
  (unresolved-table-ids [_] nil)
  (resolve-fields [_ _] nil)
  (resolve-tables [_ _ ] nil)

  Object
  (unresolved-field-ids [_] nil)
  (unresolved-table-ids [_] nil)
  (resolve-fields [this _] this)
  (resolve-tables [this _] this)

  clojure.lang.Sequential
  (unresolved-field-ids [this]
    (reduce set/union (map unresolved-field-ids this)))
  (unresolved-table-ids [this]
    (reduce set/union (map unresolved-table-ids this)))
  (resolve-fields [this field-id->field]
    (into (empty this) (for [item this]
                         (resolve-fields item field-id->field))))
  (resolve-tables [this table-id->table]
    (into (empty this) (for [item this]
                         (resolve-tables item table-id->table))))

  clojure.lang.IPersistentMap
  (unresolved-field-ids [this]
    (unresolved-field-ids (vals this)))
  (unresolved-table-ids [this]
    (unresolved-table-ids (vals this)))
  (resolve-fields [this field-id->field]
    (into this (for [[k v] this]
                 [k (resolve-fields v field-id->field)])))
  (resolve-tables [this table-id->table]
    (into this (for [[k v] this]
                 [k (resolve-tables v table-id->table)])))

  FieldID
  (unresolved-field-ids [{:keys [id]}]
    #{id})
  (unresolved-table-ids [_] nil)
  (resolve-fields [{:keys [id], :as this} field-id->field]
    (field-id->field id this))
  (resolve-tables [this _] this)

  TableID
  (unresolved-field-ids [_] nil)
  (unresolved-table-ids [{:keys [id]}] #{id})
  (resolve-fields [this _] this)
  (resolve-tables [{:keys [id], :as this} table-id->table]
    (table-id->table id this)))

(s/defn fetch-and-resolve-fields :- Query
  [query remaining-passes]
  (println "[fields] remaining-passes:" remaining-passes) ; NOCOMMIT
  (when (zero? remaining-passes)
    (throw (Exception. "Could not resolve all fields.")))
  (let [ids (unresolved-field-ids query)]
    (if-not (seq ids)
      query
      (let [field-id->field (u/key-by :id (db/select Field :id [:in ids]))]
        (recur (resolve-fields query field-id->field)
               (dec remaining-passes))))))

(s/defn fetch-and-resolve-tables :- Query
  [query remaining-passes]
  (println "[tables] remaining-passes:" remaining-passes) ; NOCOMMIT
  (when (zero? remaining-passes)
    (throw (Exception. "Could not resolve all tables.")))
  (let [ids (unresolved-table-ids query)]
    (if-not (seq ids)
      query
      (let [table-id->table (u/key-by :id (db/select Table :id [:in ids]))]
        (recur (resolve-tables query table-id->table)
               (dec remaining-passes))))))

(s/defn resolve :- Query
  [query :- Query]
  (-> query
      (fetch-and-resolve-fields 5)
      (fetch-and-resolve-tables 5)))
