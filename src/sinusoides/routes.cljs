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

(defn goto! [app page]
  (swap! app assoc-in [:last] (:view @app))
  (swap! app assoc-in [:view] page))

(defn goto-do! [app id]
  (goto! app [:do])
  (if (nil? id)
    (do (swap! app assoc-in [:do :last] nil)
        (swap! app assoc-in [:do :detail] nil))
    (do (swap! app assoc-in [:do :last] (get-in @app [:do :detail]))
        (swap! app assoc-in [:do :detail] id))))

(defn make-routes! [app]
  (defroute am "/am" []
    (goto! app [:am]))
  (defroute do "/do" []
    (goto-do! app nil))
  (defroute do- "/do/:id" [id]
    (goto-do! app id))
  (defroute think "/think" []
    (goto! app [:todo]))
  (defroute todo "/todo" []
    (goto! app [:todo]))
  (defroute main "/" []
    (goto! app [:main]))
  (defroute not-found "/dead-end" []
    (goto! app [:not-found])))

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
