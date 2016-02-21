(ns sinusoides.routes
  (:require [sinusoides.util :as util]
            [cljs.core.match]
            [clojure.string :as string]
            [secretary.core :as secretary :refer-macros [defroute]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [pushy.core :as pushy])
  (:import goog.History))

(def history (atom))

(defn make-routes! [app]
  (defroute am "/am" [] (om/update! app :view [:am]))
  (defroute do "/do" []
    (om/update! app :view [:do])
    (om/update! app [:do :detail] nil))
  (defroute do- "/do/:id" [id]
    (om/update! app :view [:do])
    (om/update! app [:do :detail] id))
  (defroute think "/think" [] (om/update! app :view [:think]))
  (defroute todo "/todo" [] (om/update! app :view [:todo]))
  (defroute main "/" [] (om/update! app :view [:main])))

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

(defn router [app owner]
  (reify
    om/IWillMount
    (will-mount [_] (make-routes! app))
    om/IRender
    (render [_] (dom/div nil))))

(defn init-router! [state]
  (om/root router state
    {:target (.getElementById js/document "router")})
  (init-history!))

(defn nav! [token]
  (pushy/set-token! @history token))
