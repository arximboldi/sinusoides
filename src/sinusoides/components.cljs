;; Copyright (c) 2011-2016 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
;;
;; This file is part of Sinusoid.es.
;;
;; Sinusoid.es is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; Sinusoid.es is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Sinusoid.es.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns sinusoides.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs-http.client :as http]
            [cljs.core.match :refer-macros [match]]
            [goog.events :as events]
            [cljs.core.match]
            [cljsjs.showdown]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn init-view []
  [:div "..."])

(defn not-found-view []
  (let [hover (r/atom false)]
      (fn []
        [:div#not-found-page
         [:div#sinusoid.imglink {:class (when @hover "hovered")} [:div] [:div]]
         [:div#the-end {:class (when @hover "hovered")}]
         [:a#dead-end {:href (routes/main)
                       :on-mouse-over #(reset! hover true)
                       :on-mouse-out #(reset! hover false)}
          [:span.first "Dead"] [:br]
          [:span.second "end"]]])))

(defn todo-view []
  (r/with-let [hover (r/atom false)]
    [:div#todo-page
     [:a {:href (routes/main)
          :on-mouse-over #(reset! hover true)
          :on-mouse-out #(reset! hover false)}
      [:div#sinusoid.imglink [:div] [:div]]]
     [:div#todo-block.links {:class (when @hover "hovered")}
      "TO" [:a {:href (routes/do)} "DO."]]]))

(defn main-view []
  (r/with-let [gens [#(rand-nth ["What "
                                 "Who "
                                 "Where "
                                 "Why "])
                     #(rand-nth [" I"
                                 " you"
                                 " they"])
                     #(rand-nth [[" I " "am"]
                                 [" you " "are"]
                                 [" they " "are"]])]
               hover (r/atom false)
               parts (r/atom ;; (vec (map apply gens))
                       ["What " " you" [" I " "am"]])
               randomize (fn []
                           (let [[idx gen] (rand-nth (map-indexed vector gens))]
                             (swap! parts #(assoc % idx (gen)))))
               interval (.setInterval js/window randomize 5000)]
    [:div#main-page
     [:a {:href (routes/not-found)
            :on-mouse-over #(reset! hover true)
          :on-mouse-out #(reset! hover false)}
      [:div#sinusoid.imglink [:div] [:div]]]
     [:div#barcode]
     [:a {:href (routes/todo)} [:div#barcode2]]
     [:div#main-text.links {:class (when @hover "hovered")}
      [:div#main-pre-text (@parts 0)
       [:a {:href (routes/do)} "do"] (@parts 1)]
      [:div#main-post-text
       [:a {:href (routes/think)} "think"] ((@parts 2) 0)
       [:a {:href (routes/am)} ((@parts 2) 1)] "?"]]
     [:div#fingerprint.links
      [:a {:href (str "mailto:"
                   (s/reverse "gro.ung@vokinloksar"))}
       "CE3E CB30 6F40 3D98 DB2E" [:br]
       "B65C 529B A962 690A 70B1"]]]
    (finally (.clearInterval js/window interval))))

(defn md->html [str]
  (let [Converter (.-converter js/Showdown)
        converter (Converter.)]
    (.makeHtml converter str)))

(defn am-view [am]
  (r/with-let [_ (go (let [response (<! (http/get "/data/am.json"))]
                       (reset! am (vec (shuffle (:body response))))))
               rand-px #(str (rand 60) "px")
               hover (r/atom false)]
    (print @am)
    [:div#am-page
     [:a {:href (routes/main)
          :on-mouse-over #(reset! hover true)
          :on-mouse-out #(reset! hover false)}
      [:div#sinusoid.imglink [:div] [:div]]]
     [:div#am-block
      {:class (when @hover "hovered")}
      [:p " I "] [:br] [:p " am "] [:br] [:p " not "]]
     [:div#profiles.links
      (for [{name :name url :url} @am]
        ^{:key name}
        [:div
         {:id name
          :style {:padding-left (rand-px)
                  :padding-top (rand-px)}}
         [:a {:href url} "this"]])]]))

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
  (r/with-let [hover (r/atom false)]
    [:div#do-page
     [:a {:href (routes/main)
          :on-mouse-over #(reset! hover true)
          :on-mouse-out #(reset! hover false)}
      [:div#sinusoid.imglink [:div] [:div]]]

     [:div#presentation.links
      {:class (when @hover "hovered")}
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
         [:span (:name p)]])]]))

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
