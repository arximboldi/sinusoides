(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs-http.client :as http]
            [cljs.core.match :refer-macros [match]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.match]))

(defn render-init []
  (html [:div "Initializing..."]))

(defn render-not-found []
  (html [:div "Not found"]))

(defn render-todo []
  (html
    [:div {:id "todo-page"}
     [:a {:href (routes/main)} [:div {:id "sinusoid"}]]
     [:div {:id "todo-block" :class "links"}
      "TO" [:a {:href (routes/do)} "DO."]]]))

(defn render-main []
  (html
    [:div {:id "main-page"}
     [:div {:id "main-block" :class "links"}
      [:div {:id "main-pre-text"}
       "What " [:a {:href (routes/do)} "do"] " you "]
      [:div {:id "main-post-text"}
       [:a {:href (routes/think)} "think"]
       " I " [:a {:href (routes/am)} "am"] "?"]
      [:a {:href (routes/todo)} [:div {:id "barcode"}]]]]))

(defn md->html [str]
  (let [Converter (.-converter js/Showdown)
        converter (Converter.)]
    (.makeHtml converter str)))

(defn am-view [am _]
  (reify
    om/IWillMount
    (will-mount [this]
      (go (let [response (<! (http/get "/data/am.json"))]
            (om/update! am (:body response)))))

    om/IRender
    (render [_]
      (html
        [:div {:id "am-page"}
         [:a {:href (routes/main)}  [:div {:id "sinusoid"}]]
         [:div {:id "am-block" :class "links"}
          [:p {:dangerouslySetInnerHTML
               {:__html "&nbsp;I&nbsp;<br/>&nbsp;am&nbsp;<br/>&nbsp;not&nbsp;<br/>"}}]]
         [:div {:id "profiles" :class "links"}
          (map (fn [{name :name url :url}]
                 [:div {:id name} [:a {:href url} "not this"]])
               am)]]))))

(defn do-view [do _]
  (reify
    om/IWillMount
    (will-mount [this]
      (go (let [response (<! (http/get "/data/do.json"))]
            (om/update! do (:body response)))))

    om/IRender
    (render [_]
      (html
        [:div {:id "do-page"}
         [:a {:href (routes/main)}  [:div {:id "sinusoid"}]]

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
            (group-by :lang @do))
          [:a {:class "cv" :href "/old/files/cv-en.pdf"} "Curriculum Vitae"]]

         [:div {:class "programs"}
          (map
             (fn [p]
               (when-let [imgs (:imgs p)]
                 [:a {:href (str "/old/" (imgs 1))}
                  [:img {:src (str "/old/" (imgs 0))}]]))
             do)]]))))

(defn root-view [app _]
  (reify om/IRender
    (render [_]
      (html
        [:div {:class "sinusoides"}
         (match [(om/value (:view app))]
           [[:am]]    (om/build am-view (:am app))
           [[:do]]    (om/build do-view (:do app))
           [[:init]]  (render-init)
           [[:main]]  (render-main)
           [[:think]] (render-todo)
           [[:todo]]  (render-todo)
           :else      (render-not-found))]))))

(defn init-components! [state]
  (om/root root-view state
    {:target (.getElementById js/document "components")}))
