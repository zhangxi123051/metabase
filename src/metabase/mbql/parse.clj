(ns metabase.mbql.parse
  "Logic for parsing MBQL queries. Function in here marked with `^:mbql` are used to do parsing."
  ;; TODO - probably don't need to exclude `filter` anymore
  (:refer-clojure :exclude [< <= > >= = != and or not filter count distinct sum min max + - / *])
  (:require [clojure.core :as core]
            [metabase.mbql :as mbql]
            [metabase.query-processor.util :as qputil]
            [metabase.util :as u]
            [metabase.util.schema :as su]
            [schema.core :as s])
  (:import [metabase.mbql
            ;; fields & values
            AbsoluteDatetime RelativeDatetime
            ;; aggregations
            AverageAggregation CountAggregation CumulativeCountAggregation CumulativeSumAggregation
            DistinctCountAggregation StandardDeviationAggregation SumAggregation MinimumAggregation MaximumAggregation
            ;; filters
            AndFilter OrFilter NotFilter EqualsFilter NotEqualsFilter LessThanFilter LessThanOrEqualFilter
            GreaterThanFilter GreaterThanOrEqualFilter BetweenFilter InsideFilter StartsWithFilter EndsWithFilter
            ContainsFilter
            ;; etc
            OrderBy
            ;; putting it all together
            Query]))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     Utils                                                      |
;;; +----------------------------------------------------------------------------------------------------------------+



;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Fields/Etc.                                                   |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defn ^:mbql field-id :- mbql/Field?
  [id :- su/IntGreaterThanZero]
  (mbql/->Field id))

(s/defn ^:mbql field :- mbql/Field?
  [x :- mbql/Field?]
  (mbql/->Field x))

(s/defn ^:mbql table-id :- mbql/Table?
  [id :- su/IntGreaterThanZero]
  (mbql/->Table id))

(s/defn ^:mbql absolute-datetime :- AbsoluteDatetime
  [x    :- mbql/AbsoluteDatetime?
   unit :- mbql/DatetimeValueUnit]
  (mbql/strict-map->AbsoluteDatetime {:timestamp (u/->Timestamp x), :unit (qputil/normalize-token unit)}))

(s/defn ^:mbql datetime-field :- mbql/DatetimeField?
  [field :- mbql/Field?
   unit  :- mbql/DatetimeFieldUnit]
  (mbql/strict-map->DatetimeField {:field (mbql/->Field field), :unit (qputil/normalize-token unit)}))



(s/defn ^:mbql relative-datetime :- RelativeDatetime
  "Value that represents a point in time relative to each moment the query is ran, e.g. \"today\" or \"1 year ago\".

   With `:current` as the only arg, refer to the current point in time; otherwise N is some number and UNIT is a unit
   like `:day` or `:year`.

     (relative-datetime :current)
     (relative-datetime -31 :day)"
  ([n :- (mbql/normalized-enum :current)]
   (relative-datetime 0 nil))

  ([n    :- s/Int
    unit :- (s/maybe mbql/DatetimeValueUnit)]
   (mbql/strict-map->RelativeDatetime
    {:amount n
     ;; give :unit a default value so we can simplify the schema a bit and require a :unit
     :unit   (if (nil? unit)
               :day
               (qputil/normalize-token unit))})))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Aggregations                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defn ^:mbql avg :- AverageAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->AverageAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql count :- CountAggregation
  ([]
   (mbql/strict-map->CountAggregation {:field nil}))
  ([field :- mbql/Field?]
   (mbql/strict-map->CountAggregation {:field (mbql/->Field field)})))

(s/defn ^:mbql cum-count :- CumulativeCountAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->CumulativeCountAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql cum-sum :- CumulativeSumAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->CumulativeSumAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql distinct :- DistinctCountAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->DistinctCountAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql stddev :- StandardDeviationAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->StandardDeviationAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql sum :- SumAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->SumAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql min :- MinimumAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->MinimumAggregation {:field (mbql/->Field field)}))

(s/defn ^:mbql max :- MaximumAggregation
  [field :- mbql/Field?]
  (mbql/strict-map->MaximumAggregation {:field (mbql/->Field field)}))

;; TODO - empty parser for `rows`
(s/defn ^:mbql ^:private rows :- nil
  []
  ;; TODO - warn about deprecation
  nil)


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                   Breakouts                                                    |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Expressions                                                   |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                    Filters                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defn compound-filter [klass, map->filter-fn, subclauses :- [mbql/Filter?]]
  (let [subclauses (for [subclause subclauses
                         subclause (if (instance? klass subclause)
                                     (:subclauses subclause)
                                     [subclause])]
                     subclause)]
    (if-not (seq (rest subclauses))
      (first subclauses)
      (map->filter-fn {:subclauses subclauses} ))))

(s/defn ^:mbql and :- mbql/Filter?
  {:style/indent 0}
  [& subclauses]
  (compound-filter AndFilter mbql/strict-map->AndFilter subclauses))

(s/defn ^:mbql or :- mbql/Filter?
  {:style/indent 0}
  [& subclauses]
  (compound-filter OrFilter mbql/strict-map->OrFilter subclauses))

(declare = != < <= > >=)

(s/defn ^:mbql not :- mbql/Filter?
  [subclause]
  (cond
    (instance? NotFilter subclause)
    (:subclause subclause)

    (instance? NotEqualsFilter subclause)
    (= (:field subclause) (:x subclause))

    (instance? EqualsFilter subclause)
    (!= (:field subclause) (:x subclause))

    (instance? LessThanFilter subclause)
    (>= (:field subclause) (:x subclause))

    (instance? GreaterThanFilter subclause)
    (<= (:field subclause) (:x subclause))

    (instance? LessThanOrEqualFilter subclause)
    (> (:field subclause) (:x subclause))

    (instance? GreaterThanOrEqualFilter subclause)
    (< (:field subclause) (:x subclause))

    :else
    (mbql/strict-map->NotFilter {:subclause subclause})))


(defn- parse-value-for-field [field x]
  (let [field-is-datetime-field? (core/and (nil? (s/check mbql/DatetimeField? field))
                                           (nil? (s/check mbql/DatetimeValueUnit (:unit field))))
        x-is-datetime-field?     (nil? (s/check mbql/DatetimeField? x))]
    (if (core/or (core/not field-is-datetime-field?)
                 x-is-datetime-field?)
      ;; if field isn't a datetime field, or both field and x are, leave x as-is
      x
      ;; otherwise if field is a datetime field coerce x to an absolute datetime
      (absolute-datetime x (:unit field)))))

(s/defn ^:mbql = :- (s/cond-pre EqualsFilter OrFilter)
  ([field :- mbql/Field?
    x     :- mbql/Comparable?]
   (mbql/strict-map->EqualsFilter {:field (mbql/->Field field), :x (parse-value-for-field field x)}))
  ([field x & more]
   (or (= field x)
       (apply = field more))))

(s/defn ^:mbql != :- (s/cond-pre NotEqualsFilter AndFilter)
  ([field :- mbql/Field?
    x     :- mbql/Comparable?]
   (mbql/strict-map->NotEqualsFilter {:field (mbql/->Field field), :x (parse-value-for-field field x)}))
  ([field x & more]
   (and (!= field x)
        (apply != field more))))


(s/defn ^:private ordered-filter
  ([map->filter-fn, field :- mbql/Field?, x :- mbql/Orderable?]
   (map->filter-fn {:field (mbql/->Field field), :x x}))
  ([map->filter-fn field x & more]
   (and (ordered-filter map->filter-fn (mbql/->Field field) x)
        (apply ordered-filter map->filter-fn x more))))

(s/defn ^:mbql < :- (s/cond-pre LessThanFilter AndFilter)
  [field x & more]
  (apply ordered-filter mbql/strict-map->LessThanFilter field x more))

(s/defn ^:mbql <= :- (s/cond-pre LessThanOrEqualFilter AndFilter)
  [field x & more]
  (apply ordered-filter mbql/strict-map->LessThanOrEqualFilter field x more))

(s/defn ^:mbql > :- (s/cond-pre GreaterThanFilter AndFilter)
  [field x & more]
  (apply ordered-filter mbql/strict-map->GreaterThanFilter field x more))

(s/defn ^:mbql >= :- (s/cond-pre GreaterThanOrEqualFilter AndFilter)
  [field x & more]
  (apply ordered-filter mbql/strict-map->GreaterThanOrEqualFilter field x more))


(s/defn ^:mbql is-null :- EqualsFilter
  [field :- mbql/Field?]
  (= (mbql/->Field field) nil))

(s/defn ^:mbql not-null :- NotEqualsFilter
  [field :- mbql/Field?]
  (!= (mbql/->Field field) nil))

(s/defn ^:mbql between :- BetweenFilter
  [field :- mbql/Orderable?
   min   :- mbql/Orderable?
   max   :- mbql/Orderable?]
  (mbql/strict-map->BetweenFilter {:field (mbql/->Field field), :min min, :max max}))


(s/defn ^:mbql inside :- InsideFilter
  [lat     :- mbql/Field?
   lon     :- mbql/Field?
   lat-max :- mbql/Orderable?
   lon-min :- mbql/Orderable?
   lat-min :- mbql/Orderable?
   lon-max :- mbql/Orderable?]
  (mbql/strict-map->InsideFilter {:lat     (mbql/->Field lat)
                                  :lon     (mbql/->Field lon)
                                  :lat-min lat-min
                                  :lat-max lat-max
                                  :lon-min lon-min
                                  :lon-max lon-max}))


(s/defn ^:mbql starts-with :- StartsWithFilter
  [field :- mbql/Field?, s :- s/Str]
  (mbql/strict-map->StartsWithFilter {:field (mbql/->Field field), :s s}))

(s/defn ^:mbql ends-with :- EndsWithFilter
  [field :- mbql/Field?, s :- s/Str]
  (mbql/strict-map->EndsWithFilter {:field (mbql/->Field field), :s s}))

(s/defn ^:mbql contains :- ContainsFilter
  [field :- mbql/Field?, s :- s/Str]
  (mbql/strict-map->ContainsFilter {:field (mbql/->Field field), :s s}))

(s/defn ^:mbql does-not-contain :- NotFilter
  [field :- mbql/Field?, s :- s/Str]
  (not (contains field s)))


(s/defn ^:mbql time-interval :- mbql/Filter?
  "Filter subclause. Syntactic sugar for specifying a specific time interval.

    ;; return rows where datetime Field 100's value is in the current day
    (filter {} (time-interval (field-id 100) :current :day)) "
  [f    :- mbql/Field?
   n    :- (s/cond-pre s/Keyword s/Str s/Num)
   unit :- (s/cond-pre s/Keyword s/Str)]
  (if-not (integer? n)
    (case (qputil/normalize-token n)
      :current (recur f  0 unit)
      :last    (recur f -1 unit)
      :next    (recur f  1 unit))
    (let [f (datetime-field f unit)]
      (cond
        (core/= n  0) (= f (relative-datetime  0 unit))
        (core/= n -1) (= f (relative-datetime -1 unit))
        (core/= n  1) (= f (relative-datetime  1 unit))
        (core/< n -1) (between f (relative-datetime  n unit)
                                 (relative-datetime -1 unit))
        (core/> n  1) (between f (relative-datetime  1 unit)
                                 (relative-datetime  n unit))))))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                    Order By                                                    |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defn ^:mbql asc :- OrderBy
  [field :- mbql/Field?]
  (mbql/strict-map->OrderBy {:field (mbql/->Field field), :direction :asc}))

(s/defn ^:mbql desc :- OrderBy
  [field :- mbql/Field?]
  (mbql/strict-map->OrderBy {:field (mbql/->Field field), :direction :desc}))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                      Page                                                      |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Source Query                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     Query                                                      |
;;; +----------------------------------------------------------------------------------------------------------------+

(def ^:private mbql-fns
  (into {} (for [[symb varr] (ns-interns *ns*)
                 :when (:mbql (meta varr))]
             [(keyword symb) varr])))

(defn- parse-clause [x]
  (if-not (sequential? x)
    ;; if x is not sequential, there's nothing to do here
    x
    (let [[token & more] x]
      ;; otherwise if it *is* sequential and starts with an MBQL token, recursively parse the args and dispatch to
      ;; approriate parser fn
      (if-let [mbql-fn (when ((some-fn keyword? string?) token)
                         (mbql-fns (qputil/normalize-token token)))]
        (do
          (println "fn call:" (cons mbql-fn more))
          (apply mbql-fn (map parse-clause more)))
        ;; otherwise if it's sequential but not an MBQL clause then just recursively parse all the items in it
        (map parse-clause x)))))

(defn- parse-sequence-with [f args]
  (when (seq args)
    (map f args)))

(defn- handle-aggregations [ags]
  (when (seq ags)
    ;; if ags is a single aggregation (i.e., is not a sequence of aggregation clauses) then wrap it.
    ;; This was it's always a sequence.
    (let [ags (if-not (s/check [mbql/Aggregation?] ags)
                ags
                [ags])]
      (core/filter identity ags))))

(def ^:private special-clause-parsers
  "Functions that get called on the values for top-level MBQL keys. For example, if a query specifies an
  :aggregation key, `handle-aggregations` will get called on the value for that key, after it's been parsed
  as MBQL itself. This is primarily a chance to do things like coerce arguments to Fields or other cleanup."
  {:aggregation  handle-aggregations
   :breakout     (partial parse-sequence-with mbql/->Field)
   :fields       (partial parse-sequence-with mbql/->Field)
   :source-table mbql/->Table
   :order-by     (partial parse-sequence-with (fn [arg]
                                                (println "order-by-clause" arg) ; NOCOMMIT
                                                arg
                                                ))})


(s/defn parse :- Query
  {:style/indent 0}
  [query :- su/Map]
  (mbql/map->Query
   (into {} (for [[k v] query]
              (let [k                     (qputil/normalize-token k)
                    special-clause-parser (special-clause-parsers k identity)]
                [k (special-clause-parser (parse-clause v))])))))

#_(s/defn query :- Query
  [& args]
  ((apply comp args) (mbql/map->Query {})))
