(ns sinusoides.routes
  (:require [sinusoides.util :as util]
            [cljs.core.match]
            [goog.events :as events]
            [secretary.core :as secretary :refer-macros [defroute]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:import goog.History))

(defn make-routes! [app]
  (defroute am "/am" [] (om/update! app :view [:am]))
  (defroute do "/do" [] (om/update! app :view [:do]))
  (defroute think "/think" [] (om/update! app :view [:think]))
  (defroute todo "/todo" [] (om/update! app :view [:todo]))
  (defroute root "/" [] (om/update! app :view [:main])))

(defn init-history! []
  (secretary/set-config! :prefix "#")
  (doto (History.)
    (goog.events/listen "navigate" #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

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
