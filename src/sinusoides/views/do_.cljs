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

(ns sinusoides.views.do-
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util :refer-macros [match-get]]
            [sinusoides.routes :as routes]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.slideshow :as slideshow]
            [sinusoides.views.decorators :as deco]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [goog.events :as events]
            [reagent.core :as r]))

(def state
  {:entries []
   :languages []
   :tags []
   :filter {:languages #{}
            :tags #{}}})

(defn detail-view [item]
  [:div.do-detail.links
   [:div.left-side
    [:a.img {:href (str "/static/screens/" ((:imgs item) 1))
             :style {:background-image
                     (str "url(/static/screens/" ((:imgs item) 1) ")")}}]]
   [:div.right-side
    [:div.content
     [:div.header {:dangerouslySetInnerHTML
                   {:__html (:name item)}}]
     [:div.desc {:dangerouslySetInnerHTML
                 {:__html (util/md->html (:desc item))}}]
     (for [link (:link item)]
       ^{:key link}
       [:a.link {:href (:href link)} (:name link)])]]])

(defn filters-view [do]
  (r/with-let
    [aliases {"CoffeeScript" "Coffee"}

     clear-filters
     (fn []
       (swap! do assoc-in [:filter] {:languages #{}
                                     :tags #{}}))
     toggle-filter
     (fn [type name]
       (swap! do update-in [:filter type]
              #(util/togglej % name)))

     filter-view
     (fn [type name]
       [:div.filter
        {:class (if (contains? (get-in @do [:filter type]) name)
                  "on" "off")
         :on-click #(toggle-filter type name)}
        (or (get aliases name) name)])]

    [:div.filters
     [:div.filter-group
      [:div.header "//"]
      [:a.cv {:href "/static/files/resume-en.pdf"} "Résumé"]
      (when-not (= (get-in @do [:filter]) {:languages #{}
                                           :tags #{}})
        [:div.filter.clearf {:on-click clear-filters} "Clear"])]

     [:div.filter-group
      [:div.header "//"]
      (for [tag (:tags @do)]
        ^{:key tag}
        [filter-view :tags tag])]

     [:div.filter-group
      [:div.header "//"]
      (for [lang (:languages @do)]
        ^{:key lang}
        [filter-view :languages lang])]]))

(defn programs-view [entries]
  [deco/grid {:class "programs"
              :grid-size-em   10.25
              :grid-margin-em  0.375}
   (for [p @entries]
     (let [background-style
           {:background-image
            (str "url(\"/static/screens/" ((:imgs p) 0) "\")")}]
       ^{:key (:slug p)}
       [:a {:href (routes/do- {:id (:slug p)})
            :style background-style}
        [:div {:style background-style}]
        [:span {:dangerouslySetInnerHTML {:__html (:name p)}}]]))])

(defn view [sin do view last]
  (r/with-let
    [as-vec    #(if (vector? %) % [%])
     get-langs #(as-vec (:lang %))
     get-tags  #(as-vec (:tags %))

     _
     (go (let [entries   (map #(assoc % :slug (util/to-slug (:name %)))
                              (:body  (<! (http/get "/data/do.json"))))
               languages (apply sorted-set
                                (filter identity (mapcat get-langs entries)))
               tags      (apply sorted-set
                                (filter identity (mapcat get-tags entries)))]
           (swap! do assoc-in [:entries] entries)
           (swap! do assoc-in [:languages] languages)
           (swap! do assoc-in [:tags] tags)))

     contain-any?
     (fn [set v]
       (some #(contains? set %) v))

     entries
     (r/track
       (fn []
         (let [lang-filters (get-in @do [:filter :languages])
               tags-filters (get-in @do [:filter :tags])]
           (vec (filter #(and
                           (or (empty? tags-filters)
                               (contain-any? tags-filters
                                             (get-tags %)))
                           (or (empty? lang-filters)
                               (contain-any? lang-filters
                                             (get-langs %))))
                        (:entries @do))))))

     slideshow
     (r/track
       (fn []
         {:route-item #(routes/do- {:id %})
          :route-back #(routes/do)
          :curr (match-get @view [:do id])
          :last (match-get @last [:do id])
          :entries @entries}))]

    [:div#do-page.page
     [:div#presentation.links (sinusoid/hovered sin)
      [:div.title "Do."]]

     [:div#stuff
      [filters-view do]
      [programs-view entries]]

     [slideshow/view slideshow detail-view]]))
