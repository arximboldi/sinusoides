(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [cljs.core.match]))

(defn render-init []
  (dom/div "Initializing..."))

(defn render-not-found []
  (dom/div "Not found"))

(defn root-view [app _]
  (reify om/IRender
    (render [_]
      (dom/div {:class "sinusoides"}
        (dom/h1 nil (dom/a {:href (routes/root)} nil "sinusoid.es"))
        (match [(om/value (:view app))]
          [[:init]] (render-init)
          :else (render-not-found))))))

(defn init-components! [state]
  (om/root root-view state
    {:target (.getElementById js/document "components")}))
