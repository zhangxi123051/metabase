(ns metabase.api.upload
  "/api/upload endpoints."
  (:require [compojure.core :refer [GET POST PUT DELETE]]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [metabase.api.common :as api]
            [metabase.automagic-dashboards
             [core :as magic]]
            [metabase.driver :as driver]
            [metabase.events :as events]
            [metabase.models
             [database :refer [Database]]
             [table :refer [Table]]]
            [toucan [db :as db]]))

(def uploads-db {:classname   "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname     "uploads.db"})

(defn create-table
  [table-name-prefix specs]
  (let [table-name (keyword (str/replace (str table-name-prefix " " (new java.util.Date)) #"[^a-zA-Z0-9_]" "_"))
        ddl (jdbc/create-table-ddl table-name specs)]
    (log/info (str ddl))
    (jdbc/db-do-commands uploads-db ddl)
    table-name))

(defn create-or-sync-db
  []
  (let [db (db/select-one Database :name "Uploads" :engine "sqlite")]
    (if db
      (do (events/publish-event! :database-trigger-sync db)
        db)
      (let [new-db (db/insert! Database :name "Uploads" :engine "sqlite" :details {:db "uploads.db"})]
        (events/publish-event! :database-create new-db)
        new-db))))

; https://www.sqlite.org/syntax/numeric-literal.html
(def sqlite-integer-regex #"(?i)^\s*([+-]?(\d+)|0x[0-9a-f]+)\s*$")
(def sqlite-real-regex    #"(?i)^\s*([+-]?(\d+(\.\d*)?|\.\d+)(e[+-]?\d+)?)\s*$")

(defn detect-col-type
  [rows col-index col-name]
  ; check the first 100 non empty cells
  (let [non-nil-values (take 100 (filter not-empty (map #(nth % col-index) rows)))]
    (cond
      (every? #(re-matches sqlite-integer-regex %) non-nil-values) :integer
      (every? #(re-matches sqlite-real-regex %)    non-nil-values) :real
      :else                                                        :text)))

(defn detect-col-types
  [col-names rows]
  (map-indexed (partial detect-col-type rows) col-names))

; ["", "_1", "_2", ...]
(def unique-suffixes (concat [""] (map #(str "_" (+ 1 %)) (range))))

(defn unique-names
  [names]
  (loop [unique-names    []
         remaining-names (map clojure.string/lower-case names)
         seen-names      #{}]
    (if (empty? remaining-names)
      unique-names
      (let [candidates (map #(str (first remaining-names) %) unique-suffixes)
            name       (first (filter #(not (contains? seen-names %)) candidates))]
        (recur (conj unique-names name)
               (rest remaining-names)
               (conj seen-names name))))))

(defn wait-for
  [f]
  (loop []
    (or (f)
        (do (log/info "sleeping")
            (Thread/sleep 1000)
            (recur)))))

(defn wait-for-table
  [table-name]
  (wait-for #(db/select-one Table :name (name table-name))))

(defn wait-for-automagic-dashboard
  [table-id]
  (wait-for #(magic/automagic-dashboard (Table table-id))))

(api/defendpoint POST "/upload_table"
  "Add a file to the Uploaded tables"
  [:as {body :body}]
  (let [parsed     (csv/read-csv (slurp body))
        rows       (rest parsed)
        col-names  (map keyword (unique-names (first parsed)))
        col-types  (detect-col-types col-names rows)
        col-specs  (map vector col-names col-types)
        table-name (create-table "Uploaded" col-specs)
        results    (jdbc/insert-multi! uploads-db table-name col-names rows)
        db         (create-or-sync-db)
        table      (wait-for-table table-name)
        ad         (wait-for-automagic-dashboard (:id table))]
      {:status   "ok"
       :db_id    (:id db)
       :table_id (:id table)}))

(api/define-routes)
