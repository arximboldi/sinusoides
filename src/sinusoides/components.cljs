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

(defn render-main []
  (dom/div {:id "backlink-nohover"}
    (dom/div {:id "main-block" :class "links"}
      (dom/div {:id "main-pre-text"}
        "What " (dom/a {:href (routes/do)} "do") " you ")
      (dom/div {:id "main-post-text"}
        (dom/a {:href (routes/think)} "think")
        " I " (dom/a {:href (routes/am)} "am"))
      (dom/a {:href (routes/todo)} (dom/div {:id "barcode"})))))

(defn root-view [app _]
  (reify om/IRender
    (render [_]
      (dom/div {:class "sinusoides"}
        (match [(om/value (:view app))]
          [[:init]] (render-init)
          :else (render-not-found))))))
           [[:main]] (render-main)

(defn init-components! [state]
  (om/root root-view state
    {:target (.getElementById js/document "components")}))
