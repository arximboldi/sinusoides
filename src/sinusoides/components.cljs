(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs-http.client :as http]
            [cljs.core.match :refer-macros [match]]
            [goog.events :as events]
            [cljs.core.match]
            [cljsjs.showdown]
            [reagent.core :as r]))

(defn init-view []
  [:div])

(defn not-found-view []
  [:div "Not found"])

(defn todo-view []
  [:div#todo-page
   [:a {:href (routes/main)} [:div#sinusoid.imglink [:div] [:div]]]
   [:div#todo-block.links "TO" [:a {:href (routes/do)} "DO."]]])

(defn main-view []
  [:div#main-page
   [:div#main-block.links
    [:div#main-pre-text "What " [:a {:href (routes/do)} "do"] " you "]
    [:div#main-post-text
     [:a {:href (routes/think)} "think"] " I "
     [:a {:href (routes/am)} "am"] "?"]
    [:a {:href (routes/todo)}
     [:div#barcode.imglink
      [:img {:src "/static/pic/barcode-s-c.png"}]
      [:img {:src "/static/pic/barcode-s-c-red.png"}]]]]])

(defn md->html [str]
  (let [Converter (.-converter js/Showdown)
        converter (Converter.)]
    (.makeHtml converter str)))

(defn am-view [am]
  (go (let [response (<! (http/get "/data/am.json"))]
        (reset! am (:body response))))

  (fn [am]
    [:div#am-page
     [:a {:href (routes/main)} [:div#sinusoid.imglink [:div] [:div]]]
     [:div#am-block [:p " I " [:br] " am " [:br] " not " [:br]]]
     [:div#profiles.links
      (for [{name :name url :url} @am]
        ^{:key name}
        [:div {:id name} [:a {:href url} "not this"]])]]))

(defn do-detail-view [entries entry]
  (letfn
      [(nav-arrow! [diff]
         (when-let [[idx _] @entry]
           (when-let [next (get-in @entries [(+ idx diff) :slug])]
             (routes/nav! (routes/do- {:id next})))))

       (handle-key [event]
         (case (.-keyCode event)
           37 (nav-arrow! -1)
           39 (nav-arrow! +1)
           27 (routes/nav! (routes/do))
           nil))]

    (r/with-let [listener (events/listen js/document "keydown" handle-key)]
      (let [[idx p] @entry]
        [:div#do-detail.links
         [:a#img {:href (str "/static/screens/" ((:imgs p) 1))
                  :style {:background-image
                          (str "url(/static/screens/" ((:imgs p) 1) ")")}}]
         [:div#body
          [:div#content
           [:div#header (:name p)]
           [:div#desc {:dangerouslySetInnerHTML
                       {:__html (md->html (:desc p))}}]
           (for [link (:link p)]
             ^{:key link}
             [:a.link {:href (:href link)} (:name link)])]
          [:div#footer
           (if-let [id (get-in @entries [(- idx 1) :slug])]
             [:a.prev.enabled {:href (routes/do- {:id id})}]
             [:div.prev.disabled])
           (if-let [id (get-in @entries [(+ idx 1) :slug])]
             [:a.next.enabled {:href (routes/do- {:id id})}]
             [:div.next.disabled])
           [:a.close {:href (routes/do)}]]]])
      (finally (events/unlistenByKey listener)))))

(defn do-view- [do entries]
  [:div#do-page
   [:a {:href (routes/main)}
    [:div#sinusoid.imglink [:div] [:div]]]
   [:div#presentation.links
    [:div.title "Do."]
    [:div.intro
     [:a {:href (routes/am)} "Being"]
     " is doing. One thing that I do a lot is building and talking
         about software. Most of it is "
     [:a {:href "http://www.gnu.org/philosophy/free-sw.html"}
      "libre software"]
     ". Libre software is a nice "
     [:a {:href "todo.html"} "thought"],
     " that blurs the boundaries between consumers and producers of
     software."
     [:em " Blah blah."]
     "You can taste a selection of my doing here."]]

   [:div#language-links
    [:a.cv {:href "/static/files/resume-en.pdf"} "Résumé"]
    (doall
      (for [lang (:languages @do)]
        ^{:key lang}
        [:div.filter
         {:class (if (contains? (get-in @do [:filter :languages]) lang)
                   "on"
                   "off")
          :on-click
          (fn [] (swap! do update-in [:filter :languages]
                   #(util/togglej % lang)))}
         lang]))
    (when-not (empty? (get-in @do [:filter :languages]))
      [:div.filter.clearf
       {:on-click (fn [] (swap! do assoc-in [:filter :languages] #{}))}
       "Clear"])]

   [:div.programs
    (for [p @entries]
      ^{:key p}
      [:a {:href (routes/do- {:id (:slug p)})
           :style {:background-image
                   (str "url(\"/static/screens/" ((:imgs p) 0) "\")")}}
       [:div]
       [:span (:name p)]])]])

(defn do-view [do]
  (letfn
      [(filter-entries []
         (let [lang-filters (get-in @do [:filter :languages])]
           (vec (filter #(or (empty? lang-filters)
                           (contains? lang-filters (:lang %)))
                  (:entries @do)))))

       (find-entry [entries id]
         (when id
           (first (filter #(= id (:slug (% 1)))
                    (map-indexed vector entries)))))

       (fetch-data []
         (go (let [entries   (map #(assoc % :slug (util/to-slug (:name %)))
                               (:body  (<! (http/get "/data/do.json"))))
                   languages (apply sorted-set (map :lang entries))]
               (swap! do assoc-in [:entries] entries)
               (swap! do assoc-in [:languages] languages))))]

    (fetch-data)
    (r/with-let [entries (r/track filter-entries)
                 detail  (r/track #(:detail @do))
                 entry   (r/track #(find-entry @entries @detail))]
      (if @entry
        [do-detail-view entries entry]
        [do-view- do entries]))))

(defn root-view [app]
  [:div.sinusoides
   (match [(:view @app)]
     [[:am]]    [am-view (r/cursor app [:am])]
     [[:do]]    [do-view (r/cursor app [:do])]
     [[:init]]  [init-view]
     [[:main]]  [main-view]
     [[:think]] [todo-view]
     [[:todo]]  [todo-view]
     :else      [not-found-view])])

(defn init-components! [state]
  (r/render-component
    [root-view state]
    (.getElementById js/document "components")))
