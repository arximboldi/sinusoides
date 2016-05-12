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
            [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.views.slideshow :as slideshow]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [goog.events :as events]
            [reagent.core :as r]))

(def state
  {:entries []
   :languages []
   :filter {:languages #{}}})

(defn detail-view [item]
  [:div.do-detail.links
   [:div.left-side
    [:a.img {:href (str "/static/screens/" ((:imgs item) 1))
             :style {:background-image
                     (str "url(/static/screens/" ((:imgs item) 1) ")")}}]]
   [:div.right-side
    [:div.content
     [:div.header (:name item)]
     [:div.desc {:dangerouslySetInnerHTML
                 {:__html (util/md->html (:desc item))}}]
     (for [link (:link item)]
       ^{:key link}
       [:a.link {:href (:href link)} (:name link)])]]])

(defn languages-view [do]
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
      "Clear"])])

(defn programs-view [entries]
  [:div.programs
   (for [p @entries]
     ^{:key (:slug p)}
     [:a {:href (routes/do- {:id (:slug p)})
          :style {:background-image
                  (str "url(\"/static/screens/" ((:imgs p) 0) "\")")}}
      [:div]
      [:span (:name p)]])])

(defn view [sin do view last]
  (r/with-let
    [get-langs
     (fn [p]
       (let [lang (:lang p)]
         (if (vector? lang)
           lang
           [lang])))

     _
     (go (let [entries   (map #(assoc % :slug (util/to-slug (:name %)))
                              (:body  (<! (http/get "/data/do.json"))))
               languages (apply sorted-set
                                (filter identity (mapcat get-langs entries)))]
           (swap! do assoc-in [:entries] entries)
           (swap! do assoc-in [:languages] languages)))

     contain-any?
     (fn [set v]
       (some #(contains? set %) v))

     entries
     (r/track
       (fn []
         (let [lang-filters (get-in @do [:filter :languages])]
           (vec (filter #(or (empty? lang-filters)
                             (contain-any? lang-filters
                                           (get-langs %)))
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
      [:div.title "Do."]
      [:div.intro
       [:a {:href (routes/am)} "Being"]
       " is doing. One thing that I do a lot is building and talking
         about software. Most of it is "
       [:a {:href "http://www.gnu.org/philosophy/free-sw.html"}
        "libre software"]
       ". Libre software is a nice "
       [:a {:href (routes/think)} "thought"],
       " that blurs the boundaries between consumers and producers of
     software."
       [:em " Blah blah."]
       "You can taste a selection of my doing here."]]

     [:div#stuff
      [languages-view do]
      [programs-view entries]]

     [slideshow/view slideshow detail-view]]))
