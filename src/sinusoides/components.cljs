(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs-http.client :as http]
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

(defn do-view [do _]
  (reify
    om/IWillMount
    (will-mount [this]
      (go (let [response (<! (http/get "/data/do.json"))]
            (om/update! do (group-by :lang (:body response))))))

    om/IRender
    (render [_]
      (html
        [:div
         [:a {:href (routes/main)}  [:div {:id "backlink"}]]
         [:div {:id "presentation" :class "links"}
          [:div {:class "title"} "Do."]
          [:div {:class "intro"}
           [:a {:href (routes/am)} "Being"]
           " is doing. People often do something for a living. One
           thing that I do a lot is developing software. Most of it
           is "
           [:a {:href "http://www.gnu.org/philosophy/free-sw.html"}
            "Free Software"] ".
           Because programming is fun. Because Free Software is a
           good "
           [:a {:href "todo.html"} "thought"]
           ". You can download some of the products of my doing
           here."]]

         [:div {:id "language-links"}
          (map
            (fn [[lang _]]
              [:a {:href (str "#" (util/to-slug lang))} lang])
            do)
          [:a {:class "cv" :href "/old/files/cv-en.pdf"} "Curriculum Vitae"]]

         [:div {:id "main-content"}
          (map
            (fn [[lang programs]]
              [:div {:class "language" :id (util/to-slug lang)}
               [:div {:class "title"} lang]
               [:table {:class "programs"}
                (map
                  (fn [p]
                    [:tr [:td [:div
                               (when-let [imgs (:imgs p)]
                                 [:a {:href (str "/old/" (imgs 1))}
                                  [:img {:src (str "/old/" (imgs 0))}]])
                               [:h1 [:a {:href (:link p)} (:name p)]]
                               [:p (:desc p)]]]])
                  programs)]])
            do)]]))))

(defn root-view [app _]
  (reify om/IRender
    (render [_]
      (html
        [:div {:class "sinusoides"}
         (match [(om/value (:view app))]
           [[:init]] (render-init)
           [[:todo]] (render-todo)
           [[:main]] (render-main)
           [[:do]]   (om/build do-view (:do app))
           :else (render-not-found))]))))

(defn init-components! [state]
  (om/root root-view state
    {:target (.getElementById js/document "components")}))
