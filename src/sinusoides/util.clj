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
  (:require [pl.danieljanus.tagsoup :as tagsoup]
            [cljs.core.match]))

(defmacro dbg [x]
  `(let [x# ~x]
     (println '~x "=" x#)
     x#))

(defmacro embed-svg [svg-file]
  (let [svg (tagsoup/parse-string (slurp svg-file))
        fix (let [viewbox (:viewbox (svg 1))]
              (if (nil? viewbox)
                svg
                (assoc-in svg [1 :viewBox] viewbox)))]
    `~fix))

;; from clojure.core.incubator
(defn seqable? [x]
  (or (seq? x)
      (instance? clojure.lang.Seqable x)
      (nil? x)
      (instance? Iterable x)
      (.isArray (.getClass ^Object x))
      (string? x)
      (instance? java.util.Map x)))

(defn find-symbols [pattern]
  (cond
    (symbol? pattern) [pattern]
    (seqable? pattern) (mapcat find-symbols (seq pattern))
    :else []))

(defn find-symbol-or-symbols [pattern]
  (let [syms (vec (find-symbols pattern))]
    (if (> 1 (count syms))
      syms
      (first syms))))

(defmacro match-get [arg pattern]
  `(cljs.core.match/match
     [~arg]
     [~pattern]
     ~(find-symbol-or-symbols pattern)
     :else nil))
