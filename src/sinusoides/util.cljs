(ns sinusoides.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [close! chan pipe <!]]
            [clojure.string :refer [lower-case replace]]))

(defn trace [obj & more]
  (apply prn (if more more ["TRACE:"]))
  (.dir js/console obj)
  obj)

(defn trace-debug [obj & more]
  (apply prn (if more more ["TRACE:"]))
  (.dir js/console obj)
  (js/eval "debugger")
  obj)

(defn to-slug [str]
  (-> str
    (lower-case)
    (replace #"-+" "")
    (replace #"\.+" "-")
    (replace #"\s+" "-")
    (replace #"[^a-z0-9-]" "")))

(defn togglej [set thing]
  (if (contains? set thing)
    (disj set thing)
    (conj set thing)))
