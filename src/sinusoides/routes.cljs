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

(ns sinusoides.routes
  (:require [sinusoides.util :as util]
            [cljs.core.match]
            [clojure.string :as string]
            [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy])
  (:import goog.History))

(def history (atom))

(defn make-routes! [app]
  (defroute am "/am" []
    (swap! app assoc-in [:view] [:am]))
  (defroute do "/do" []
    (swap! app assoc-in [:view] [:do])
    (swap! app assoc-in [:do :detail] nil))
  (defroute do- "/do/:id" [id]
    (swap! app assoc-in [:view] [:do])
    (swap! app assoc-in [:do :detail] id))
  (defroute think "/think" []
    (swap! app assoc-in [:view] [:todo]))
  (defroute todo "/todo" []
    (swap! app assoc-in [:view] [:todo]))
  (defroute main "/" []
    (swap! app assoc-in [:view] [:main]))
  (defroute not-found "/dead-end" []
    (swap! app assoc-in [:view] [:not-found])))

(defn- no-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (string/replace uri (re-pattern (str "^" prefix)) "")))

(defn init-history! []
  (when (aget js/window "SINUSOIDES_DEBUG_MODE")
    (print "SINUSOIDES_DEBUG_MODE enabled")
    (secretary/set-config! :prefix "/debug"))
  (let [last-dispatched (atom)
        dispatch (fn [uri]
                   (reset! last-dispatched uri)
                   (secretary/dispatch! uri))
        match-uri (fn [uri]
                    (cond
                      ;; We avoid dispatching the same URI twice.
                      ;; This allows using fragments
                      ;; in an old-school way.
                      (and (not= @last-dispatched uri)
                           (secretary/locate-route (no-prefix uri)))
                      uri
                      ;; If not route is found and we just loaded the
                      ;; page, it basically means 404
                      (nil? @last-dispatched)
                      (not-found)))]
    (swap! history #(pushy/push-state! dispatch match-uri))))

(defn init-router! [state]
  (make-routes! state)
  (init-history!))

(defn nav! [token]
  (pushy/set-token! @history token))
