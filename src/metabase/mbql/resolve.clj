(ns metabase.mbql.resolve
  (:refer-clojure :exclude [resolve])
  (:require [clojure.set :as set]
            [metabase.models
             [field :refer [Field]]
             [table :refer [Table]]]
            [metabase.util :as u]
            [schema.core :as s]
            [toucan
             [db :as db]
             [hydrate :refer [hydrate]]])
  (:import [metabase.mbql FieldID Query TableID]
           metabase.models.field.FieldInstance))

(defmulti ^:private unresolved-field-ids class)

(defmethod unresolved-field-ids nil    [_] nil)
(defmethod unresolved-field-ids Object [_] nil)

(defmulti ^:private unresolved-table-ids class)

(defmethod unresolved-table-ids nil    [_] nil)
(defmethod unresolved-table-ids Object [_] nil)

(defmulti ^:private resolve-fields (fn [this _] (class this)))

(defmethod resolve-fields nil    [_    _] nil)
(defmethod resolve-fields Object [this _] this)

(defmulti ^:private resolve-tables (fn [this _] (class this)))

(defmethod resolve-tables nil    [_    _] nil)
(defmethod resolve-tables Object [this _] this)


(defmethod unresolved-field-ids clojure.lang.Sequential
  [this]
  (reduce set/union (map unresolved-field-ids this)))

(defmethod unresolved-table-ids clojure.lang.Sequential
  [this]
  (reduce set/union (map unresolved-table-ids this)))

(defmethod resolve-fields clojure.lang.Sequential
  [this field-id->field]
  (into (empty this) (for [item this]
                       (resolve-fields item field-id->field))))

(defmethod resolve-tables clojure.lang.Sequential
  [this table-id->table]
  (into (empty this) (for [item this]
                       (resolve-tables item table-id->table))))


(defmethod unresolved-field-ids clojure.lang.IPersistentMap
  [this]
  (unresolved-field-ids (vals this)))

(defmethod unresolved-table-ids clojure.lang.IPersistentMap
  [this]
  (unresolved-table-ids (vals this)))

(defmethod resolve-fields clojure.lang.IPersistentMap
  [this field-id->field]
  (into this (for [[k v] this]
               [k (resolve-fields v field-id->field)])))

(defmethod resolve-tables clojure.lang.IPersistentMap
  [this table-id->table]
  (into this (for [[k v] this]
               [k (resolve-tables v table-id->table)])))


(defmethod unresolved-field-ids FieldID
  [{:keys [id]}]
  #{id})

(defmethod resolve-fields FieldID
  [{:keys [id], :as this} field-id->field]
  (field-id->field id this))


(defmethod unresolved-field-ids FieldInstance
  [{parent :parent, parent-id :parent_id}]
  (when (and parent-id
             (not parent))
    #{parent-id}))

(defmethod resolve-fields FieldInstance
  [{parent :parent, parent-id :parent_id, :as this} field-id->field]
  (if (or (not parent-id)
          parent)
    this
    (assoc this :parent (field-id->field parent-id))))

(defmethod unresolved-table-ids FieldInstance
  [{table-id :table_id, table :table}]
  (when-not table
    #{table-id}))

(defmethod resolve-tables FieldInstance
  [{table-id :table_id, table :table, :as this} table-id->table]
  (if table
    this
    (assoc this :table (table-id->table table-id))))


(defmethod unresolved-table-ids TableID
  [{:keys [id]}]
  #{id})

(defmethod resolve-tables TableID
  [{:keys [id], :as this} table-id->table]
  (table-id->table id this))


(defn- fetch-fields [ids]
  (-> (db/select [Field :name :display_name :base_type :special_type :visibility_type :table_id :id :position
                  :description :fingerprint]
        :id [:in ids])
      (hydrate :values :dimensions)))

(s/defn fetch-and-resolve-fields :- Query
  [query remaining-passes field-id->field]
  (when (zero? remaining-passes)
    (throw (Exception. "Could not resolve all fields.")))
  (let [ids (unresolved-field-ids query)]
    (if-not (seq ids)
      query
      (let [field-id->field (merge field-id->field
                                   (u/key-by :id (fetch-fields ids)))]
        (recur (resolve-fields query field-id->field)
               (dec remaining-passes)
               field-id->field)))))

(s/defn fetch-and-resolve-tables :- Query
  [query remaining-passes table-id->table]
  (when (zero? remaining-passes)
    (throw (Exception. "Could not resolve all tables.")))
  (let [ids (unresolved-table-ids query)]
    (if-not (seq ids)
      query
      (let [table-id->table (merge table-id->table
                                   (u/key-by :id (db/select Table :id [:in ids])))]
        (recur (resolve-tables query table-id->table)
               (dec remaining-passes)
               table-id->table)))))

(s/defn resolve :- Query
  [query :- Query]
  (-> query
      (fetch-and-resolve-fields 5 {})
      (fetch-and-resolve-tables 5 {})))
