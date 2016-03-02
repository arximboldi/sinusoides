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
    (swap! app assoc-in [:view] [:think]))
  (defroute todo "/todo" []
    (swap! app assoc-in [:view] [:todo]))
  (defroute main "/" []
    (swap! app assoc-in [:view] [:main])))

(defn- no-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (string/replace uri (re-pattern (str "^" prefix)) "")))

(defn init-history! []
  (when (aget js/window "SINUSOIDES_DEBUG_MODE")
    (secretary/set-config! :prefix "/debug"))
  (let [last-dispatched (atom) ;; We avoid dispatching the same URI
                               ;; twice.  This allows using fragments
                               ;; in an old-school way.
        dispatch (fn [uri]
                   (reset! last-dispatched uri)
                   (secretary/dispatch! uri))
        match-uri (fn [uri]
                    (when (and (not= @last-dispatched uri)
                            (secretary/locate-route (no-prefix uri)))
                      uri))]
    (swap! history #(pushy/push-state! dispatch match-uri))))

(defn init-router! [state]
  (make-routes! state)
  (init-history!))

(defn nav! [token]
  (pushy/set-token! @history token))
