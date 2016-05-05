;; Copyright (c) 2011-2016 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
;;
;; This file is part of Sinusoid.es.
;;
;; Sinusoid.es is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; Sinusoid.es is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Sinusoid.es.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns sinusoides.util
  (:require-macros [sinusoides.util])
  (:require [cljs.core.async :refer [close! chan pipe <!]]
            [cljsjs.showdown]
            [clojure.string :refer [lower-case replace]]))

(defn md->html [str]
  (let [Converter (.-converter js/Showdown)
        converter (Converter.)]
    (.makeHtml converter str)))

(defn trace [obj & more]
  (apply prn (if more more ["TRACE:"]))
  (.dir js/console obj)
  obj)

(defn trace- [obj & more]
  (apply prn (conj (if more more ["TRACE:"]) obj))
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
