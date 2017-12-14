(ns metabase.query-processor.middleware.add-implicit-clauses
  "Middlware for adding an implicit `:fields` and `:order-by` clauses to certain queries."
  (:require [clojure.tools.logging :as log]
            [metabase.mbql.resolve :as new-resolve]
            [metabase.mbql.parse :as mbql]
            [metabase.models.field :refer [Field]]
            [metabase.query-processor
             [interface :as i]
             [sort :as sort]
             [util :as qputil]]
            [toucan
             [db :as db]
             [hydrate :refer [hydrate]]]))

(defn- fields-for-source-table
  "Return the all fields for SOURCE-TABLE, for use as an implicit `:fields` clause."
  [{{source-table-id :id, :as source-table} :source-table}]
  (map (comp mbql/field-id :id)
       (db/select [Field :id]
         :table_id        source-table-id
         :visibility_type [:not-in ["sensitive" "retired"]]
         :parent_id       nil
         {:order-by [[:position :asc]
                     [:id :desc]]})))

(defn- should-add-implicit-fields? [{:keys [fields breakout source-table], aggregations :aggregation}]
  ;; if query is using another query as its source then there will be no table to add nested fields for
  (and source-table
       (not (or (seq aggregations)
                (seq breakout)
                (seq fields)))))

(defn- add-implicit-fields [{:keys [source-table], :as inner-query}]
  (if-not (should-add-implicit-fields? inner-query)
    inner-query
    ;; this is a structured `:rows` query, so lets add a `:fields` clause with all fields from the source table +
    ;; expressions
    (let [inner-query (assoc inner-query :fields-is-implicit true)
          fields      (fields-for-source-table inner-query)
          expressions (for [[expression-name] (:expressions inner-query)]
                        (i/strict-map->ExpressionRef {:expression-name (name expression-name)}))]
      (when-not (seq fields)
        (log/warn (format "Table '%s' has no Fields associated with it." (:name source-table))))
      (assoc inner-query
        :fields (concat fields expressions)))))



(defn- add-implicit-breakout-order-by
  "`Fields` specified in `breakout` should add an implicit ascending `order-by` subclause *unless* that field is
  *explicitly* referenced in `order-by`."
  [{breakout-fields :breakout, order-by :order-by, :as inner-query}]
  (let [order-by-fields                   (set (map :field order-by))
        implicit-breakout-order-by-fields (remove order-by-fields breakout-fields)]
    (println "implicit-breakout-order-by-fields:" implicit-breakout-order-by-fields) ; NOCOMMIT
    (cond-> inner-query
      (seq implicit-breakout-order-by-fields) (update :order-by concat (for [field implicit-breakout-order-by-fields]
                                                                         (mbql/asc field))))))

(defn- add-implicit-clauses-to-inner-query [inner-query]
  (cond-> (add-implicit-fields (add-implicit-breakout-order-by inner-query))
    ;; if query has a source query recursively add implicit clauses to that too as needed
    (:source-query inner-query) (update :source-query add-implicit-clauses-to-inner-query)))

(defn- maybe-add-implicit-clauses [query]
  (if-not (qputil/mbql-query? query)
    query
    (update query :query add-implicit-clauses-to-inner-query)))

(defn add-implicit-clauses
  "Add an implicit `fields` clause to queries with no `:aggregation`, `breakout`, or explicit `:fields` clauses.
   Add implicit `:order-by` clauses for fields specified in a `:breakout`."
  [qp]
  (comp qp maybe-add-implicit-clauses))
