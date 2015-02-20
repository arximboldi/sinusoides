(ns sinusoides.routes
  (:require [sinusoides.util :as util]
            [cljs.core.match]
            [clojure.string :as string]
            [secretary.core :as secretary :refer-macros [defroute]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [pushy.core :as pushy])
  (:import goog.History))

(defn make-routes! [app]
  (defroute am "/am" [] (om/update! app :view [:am]))
  (defroute do "/do" [] (om/update! app :view [:do]))
  (defroute think "/think" [] (om/update! app :view [:think]))
  (defroute todo "/todo" [] (om/update! app :view [:todo]))
  (defroute main "/" [] (om/update! app :view [:main])))

(defn- uri-without-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (string/replace uri (re-pattern (str "^" prefix)) "")))

(defn init-history! []
  (when (aget js/window "SINUSOIDES_DEBUG_MODE")
    (secretary/set-config! :prefix "/debug"))
  (pushy/push-state! secretary/dispatch!
      (fn [x] (when (secretary/locate-route (uri-without-prefix x)) x))))

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
