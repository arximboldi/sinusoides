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
  (:require-macros [sinusoides.util]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [>!]]
            [clojure.string :as str]
            [goog.events :as events]
            [showdown]
            [fontfaceobserver]))

(defn has-touch? []
  (or (js/window.hasOwnProperty "ontouchstart")
      (pos? js/navigator.maxTouchPoints)))

(defn load-all [fn things]
  (async/into [] (async/merge (map fn things))))

(defn load-image
  ([path]
   (load-image path (async/chan)))

  ([path port]
   (let [img     (js/Image.)
         on-load (fn [] (go (>! port img)
                            (async/close! port)))]
     (set! (.-src img) path)
     (set! (.-onload img) on-load)
     port)))

(defn load-font
  ([desc]
   (load-font desc (async/chan)))

  ([desc port]
   (let [font     (js/FontFaceObserver. (:family desc) (clj->js desc))
         on-load  (fn []
                    (go (>! port font)
                        (async/close! port)))
         on-error (fn [err]
                    (print "Error while loading: " desc)
                    (print err))]
     (-> font
         (.load nil 10000)
         (.then on-load on-error))
     port)))

(defn md->html [str]
  (let [converter (js/showdown.Converter.
                    #js{"simplifiedAutoLink" true})]
    (.makeHtml converter (str/replace str "--" "—"))))

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

(defn replace-special [str]
  (let [in  "àáäâèéëêìíïîòóöôùúüûñç·/_,:;"
        out "aaaaeeeeiiiioooouuuunc------"]
    (reduce
      (fn [str [in out]]
        (str/replace str in out))
      str
      (map vector in out))))

(defn to-slug [str]
  (-> str
      (str/trim)
      (str/lower-case)
      (replace-special)
      (str/replace #"-+" "")
      (str/replace #"\.+" "-")
      (str/replace #"\s+" "-")
      (str/replace #"[^a-z0-9-]" "")))

(defn togglej [set thing]
  (if (contains? set thing)
    (disj set thing)
    (conj set thing)))
