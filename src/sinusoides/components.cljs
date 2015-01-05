(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.match]))

(defn render-init []
  (html [:div "Initializing..."]))

(defn render-not-found []
  (html [:div "Not found"]))

(defn render-todo []
  (html
    [:div :id "todo"
     [:a {:href (routes/main)} [:div {:id "backlink-vert"}]]
     [:div {:id "todo-block" :class "links"}
      "TO" [:a {:href (routes/do)} "DO."]]]))

(defn render-main []
  (html
    [:div {:id "backlink-nohover"}
     [:div {:id "main-block" :class "links"}
      [:div {:id "main-pre-text"}
       "What " [:a {:href (routes/do)} "do"] " you "]
      [:div {:id "main-post-text"}
       [:a {:href (routes/think)} "think"]
       " I " [:a {:href (routes/am)} "am"] "?"]
      [:a {:href (routes/todo)} [:div {:id "barcode"}]]]]))

(defn root-view [app _]
  (reify om/IRender
    (render [_]
      (html
        [:div {:class "sinusoides"}
         (match [(om/value (:view app))]
           [[:init]] (render-init)
           [[:todo]] (render-todo)
           [[:main]] (render-main)
           :else (render-not-found))]))))

(defn init-components! [state]
  (om/root root-view state
    {:target (.getElementById js/document "components")}))
