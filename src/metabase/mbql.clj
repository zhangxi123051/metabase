(ns metabase.mbql
  (:refer-clojure :exclude [min max])
  (:require [metabase.query-processor.util :as qputil]
            [metabase.util :as u]
            [metabase.util.schema :as su]
            [schema.core :as s])
  (:import java.sql.Timestamp))

(defn normalized-enum [& tokens]
  (let [tokens (set (map qputil/normalize-token tokens))]
    (s/pred #(contains? tokens (qputil/normalize-token %))
            tokens)))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                 MBQL Protocol                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(defprotocol MBQL
  (->mbql [this]))

(extend-protocol MBQL
  Object
  (->mbql [this] this)
  nil
  (->mbql [_] nil))

(defmethod print-method metabase.mbql.MBQL [s writer]
  (print-method (->mbql s) writer))

(defmethod clojure.pprint/simple-dispatch metabase.mbql.MBQL [s]
  (clojure.pprint/write-out (->mbql s)))

(doseq [m [print-method clojure.pprint/simple-dispatch]]
  (prefer-method m metabase.mbql.MBQL clojure.lang.IRecord)
  (prefer-method m metabase.mbql.MBQL java.util.Map)
  (prefer-method m metabase.mbql.MBQL clojure.lang.IPersistentMap))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     Fields                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(defprotocol Field
  (->Field [this]))

;; TODO - I think this should be renamed `Field` and the protocol `IField`
(def Field? (s/pred  #(satisfies? Field %) "Is a Field?"))

(s/defrecord FieldID [id :- su/IntGreaterThanZero]
  MBQL
  (->mbql [_] (list 'field-id id))
  Field
  (->Field [this] this))

(declare map->FieldID)

(extend-protocol Field
  Number
  (->Field [this] (map->FieldID {:id (long this)})))

#_(s/defrecord FKFieldID [fk-field-id   :- su/IntGreaterThanZero
                        dest-field-id :- su/IntGreaterThanZero]
  MBQL
  (->mbql [_] (list 'fk-> fk-field-id dest-field-id))
  Field
  (->Field [this] this))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                            Datetime Fields & Values                                            |
;;; +----------------------------------------------------------------------------------------------------------------+

(def datetime-field-units
  "Valid units for a `DatetimeField`."
  #{:default :minute :minute-of-hour :hour :hour-of-day :day :day-of-week :day-of-month :day-of-year
    :week :week-of-year :month :month-of-year :quarter :quarter-of-year :year})

(def relative-datetime-value-units
  "Valid units for a `RelativeDatetimeValue`."
  #{:minute :hour :day :week :month :quarter :year})

;; TODO - we should probably have a separate schema for INPUT and for what goes into the record types
(def DatetimeFieldUnit
  "Schema for datetime units that are valid for `DatetimeField` forms."
  (s/named (apply normalized-enum datetime-field-units) "Valid datetime unit for a field"))

(def DatetimeValueUnit
  "Schema for datetime units that valid for absolute or relative datetime values."
  (s/named (apply normalized-enum relative-datetime-value-units) "Valid datetime unit for a relative datetime"))


(defprotocol Datetime
  (unit [this]))

(def Datetime? (s/pred (partial satisfies? Datetime) "Is a Datetime?"))

(s/defrecord AbsoluteDatetime [timestamp :- Timestamp
                               unit      :- DatetimeValueUnit]
  MBQL
  (->mbql [_] (list 'absolute-datetime timestamp unit))
  Datetime
  (unit [_] unit))

(def AbsoluteDatetime? (s/pred (partial satisfies? u/ITimestampCoercible)
                               "Is something that can be coerced to a Timestamp?"))



(s/defrecord RelativeDatetime [amount :- s/Int
                               unit   :- DatetimeValueUnit]
  MBQL
  (->mbql [_] (list 'relative-datetime amount unit))
  Datetime
  (unit [_] unit))


(def DatetimeField? (s/both Field? Datetime?))

(s/defrecord DatetimeField [unit  :- DatetimeFieldUnit
                            field :- Field?]
  MBQL
  (->mbql [_] (list 'datetime-field (->mbql field) unit)) ; TODO - what if Field is realized?
  Field
  (->Field [this] this)
  Datetime
  (unit [_] unit))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     Tables                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(defprotocol Table
  (->Table [this]))

(def Table? (s/pred #(satisfies? Table %) "Is a Table?"))

(s/defrecord TableID [id :- su/IntGreaterThanZero]
  MBQL
  (->mbql [_] (list 'table-id id))
  Table
  (->Table [this] this))

(extend-protocol Table
  Number
  (->Table [this] (map->TableID {:id (long this)})))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                              Other Value Schemas                                               |
;;; +----------------------------------------------------------------------------------------------------------------+

(def Orderable?
  (s/named
   (s/cond-pre s/Num AbsoluteDatetime AbsoluteDatetime? Field?)
   "Something that can be sorted. A Field, number, or absoulte datetime."))

(def Comparable?
  (s/named
   (s/maybe (s/cond-pre s/Num s/Bool s/Str AbsoluteDatetime AbsoluteDatetime? Field?))
   "Something that can be compared for equality. A Field, nil, number, string, boolean, or absoute datetime."))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Aggregations                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(defprotocol Aggregation)

(def Aggregation?
  (s/pred (partial satisfies? Aggregation) "Is an aggregation clause"))

(s/defrecord AverageAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'avg (->mbql field))))

(s/defrecord CountAggregation [field :- (s/maybe Field?)]
  Aggregation
  MBQL
  (->mbql [_] (cons 'count (when field [(->mbql field)]))))

(s/defrecord CumulativeCountAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'cum-count (->mbql field))))

(s/defrecord CumulativeSumAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'cum-sum (->mbql field))))

(s/defrecord DistinctCountAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'distinct (->mbql field))))

(s/defrecord StandardDeviationAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'stddev (->mbql field))))

(s/defrecord SumAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'sum) (->mbql field)))

(s/defrecord MinimumAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'min) (->mbql field)))

(s/defrecord MaximumAggregation [field :- Field?]
  Aggregation
  MBQL
  (->mbql [_] (list 'max (->mbql field))))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                   Breakouts                                                    |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Expressions                                                   |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                    Filters                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(defprotocol Filter)

(def Filter?
  (s/pred (partial satisfies? Filter) "Is a filter clause"))

(s/defrecord AndFilter [subclauses :- [Filter?]]
  Filter
  MBQL
  (->mbql [_] (cons 'and (map ->mbql subclauses))))

(s/defrecord OrFilter [subclauses :- [Filter?]]
  Filter
  MBQL
  (->mbql [_] (cons 'or (map ->mbql subclauses))))

(s/defrecord NotFilter [subclause :- Filter?]
  Filter
  MBQL
  (->mbql [_] (list 'not (->mbql subclause))))


(s/defrecord EqualsFilter [field :- Field?
                           x     :- Comparable?]
  Filter
  MBQL
  (->mbql [_] (list '= (->mbql field) (->mbql x))))

(s/defrecord NotEqualsFilter [field :- Field?
                              x     :- Comparable?]
  Filter
  MBQL
  (->mbql [_] (list '!= (->mbql field) (->mbql x))))


(s/defrecord LessThanFilter [field :- Field?
                             x     :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list '< (->mbql field) (->mbql x))))

(s/defrecord LessThanOrEqualFilter [field :- Field?
                                    x     :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list '<= (->mbql field) (->mbql x))))

(s/defrecord GreaterThanFilter [field :- Field?
                                x     :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list '> (->mbql field) (->mbql x))))

(s/defrecord GreaterThanOrEqualFilter [field :- Orderable?
                                       x     :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list '>= (->mbql field) (->mbql x))))


(s/defrecord BetweenFilter [field :- Orderable?
                            min   :- Orderable?
                            max   :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list 'between (->mbql field) (->mbql min) (->mbql max))))


(s/defrecord InsideFilter [lat     :- Field?
                           lon     :- Field?
                           lat-min :- Orderable?
                           lat-max :- Orderable?
                           lon-min :- Orderable?
                           lon-max :- Orderable?]
  Filter
  MBQL
  (->mbql [_] (list 'inside (->mbql lat) (->mbql lon) lat-max lon-min lat-min lon-max)))


(s/defrecord StartsWithFilter [field :- Field?
                               s     :- s/Str]
  Filter
  MBQL
  (->mbql [_] (list 'starts-with (->mbql field) s)))

(s/defrecord EndsWithFilter [field :- Field?
                             s     :- s/Str]
  Filter
  MBQL
  (->mbql [_] (list 'ends-with (->mbql field) s)))

(s/defrecord ContainsFilter [field :- Field?
                             s     :- s/Str]
  Filter
  MBQL
  (->mbql [_] (list 'contains (->mbql field) s)))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                    Order By                                                    |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defrecord OrderBy [field     :- Field?
                      direction :- (s/enum :asc :desc)]
  MBQL
  (->mbql [_] (list (symbol (name direction)) (->mbql field))))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                      Page                                                      |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Source Table                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Expressions                                                   |
;;; +----------------------------------------------------------------------------------------------------------------+

(def ^:private ExpressionOperator (s/named (s/enum :+ :- :* :/) "Valid expression operator"))

(s/defrecord Expression [operator    :- ExpressionOperator
                         args        :- [(s/cond-pre Comparable?
                                                     Field
                                                     Aggregation)]
                         custom-name :- (s/maybe su/NonBlankString)]
  MBQL
  (->mbql [_]
    (let [mbql (cons operator args)]
      (if custom-name
        (list 'named mbql custom-name)
        mbql)))
  Field
  (->Field [this] this))

(s/defrecord ExpressionRef [expression-name :- su/NonBlankString]
  #_clojure.lang.Named
  #_(getName [_] expression-name)
  MBQL
  (->mbql [_] (list 'expression expression-name))
  Field
  (->Field [this] this))



;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                  Source Query                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     Query                                                      |
;;; +----------------------------------------------------------------------------------------------------------------+

(s/defrecord Query [aggregation  :- (s/maybe (su/non-empty [Aggregation?]))
                    breakout     :- (s/maybe (su/non-empty [Field?]))
                    fields       :- (s/maybe (su/non-empty [Field?]))
                    filter       :- (s/maybe Filter?)
                    limit        :- (s/maybe su/IntGreaterThanZero)
                    order-by     :- (s/maybe (su/non-empty [OrderBy]))
                    ;; page         :- (s/maybe Page)
                    ;; expressions  :- (s/maybe {s/Keyword Expression})
                    source-table :- (s/maybe Table?)
                    ;; source-query :- (s/maybe SourceQuery)
                    ]
  MBQL
  (->mbql [this]
    (into {} (for [[k v] this
                   :when (some? v)]
               [k (if (sequential? v)
                    (into (empty v) (map ->mbql v))
                    (->mbql v))]))))
