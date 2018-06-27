(ns metabase.task.DynamicClassLoadHelper
  "This is needed to get the JDBC backend for Quartz working, or something like that. See
  http://clojurequartz.info/articles/durable_quartz_stores.html for details."
  (:import clojure.lang.DynamicClassLoader
           clojure.lang.Reflector
           org.quartz.spi.ClassLoadHelper)
  (:gen-class
   :extends clojure.lang.DynamicClassLoader
   :exposes-methods {loadClass superLoadClass}
   :implements [org.quartz.spi.ClassLoadHelper]))

(defn -initialize [_])

(defn -loadClass [^metabase.task.DynamicClassLoadHelper this, ^String name, _]
  (Reflector/invokeInstanceMethod ClassLoader "loadClass" (to-array [this name])))

(defn -getClassLoader [this]
  this)
