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

(ns sinusoides.views.think
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [sinusoides.views.audio-player :as audio-player]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.slideshow :as slideshow]
            [sinusoides.views.decorators :as deco]
            [cljs-time.format :as time]
            [sinusoides.routes :as routes]
            [sinusoides.util :as util :refer-macros [match-get]]
            [clojure.string :as string]
            [cljs.core.match :refer-macros [match]]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >!]]
            [reagent.core :as r]
            [goog.events :as events]))

(def state
  {:player nil
   :entries []
   :type nil
   :data {}})

(def open-svg (util/embed-svg "resources/static/pic/open.svg"))

(defn soundcloud-player-view [thing data player & children]
  (r/with-let
    [client-id     "485230fd2a6e151244a57a584f904070"
     add-client-id #(str % "?client_id=" client-id)

     api-get
     (fn [& command]
       (http/jsonp (add-client-id (str "https://api.soundcloud.com"
                                       (apply str command)
                                       ".json"))))

     _
     (go (let [response (<! (api-get "/tracks/" (:track thing)))]
           (reset! data (:body response))))]

    (if @data
      (let [background (string/replace (:artwork_url @data)
                                       #"-large.jpg"
                                       "-t500x500.jpg")
            audio-src  (add-client-id (:stream_url @data))]
        (into
          [audio-player/view
           {:style {:background-image (str "url(" background ")")}}
           audio-src
           player]
          children))
      [:span])))

(defn sound-thumbnail-view [thing data player]
  [:div.thingy.sound
   [soundcloud-player-view thing data player
    [:a.control.open {:href (routes/think- {:id (:slug thing)})}
     open-svg]]])

(defn text-thumbnail-view [thing]
  [:a.thingy.text {:href (routes/think- {:id (:slug thing)})}
   (:title thing)])

(defn sound-detail-view [thing data player]
  (r/with-let [input-date-formatter  (time/formatter "yyyy/MM/dd hh:mm:ss Z")
               output-date-formatter (time/formatter "MMMM YYYY")]
    [:div.detail.sound
     [:div.left-side
      [soundcloud-player-view thing data player]]

     (when @data
       [:div.right-side
        [:div.name (:title @data)]
        [:div.desc {:dangerouslySetInnerHTML
                    {:__html (util/md->html (:description @data))}}]
        [:div.date (time/unparse output-date-formatter
                                 (time/parse input-date-formatter
                                             (:created_at @data)))]
        [:div.footer
         [:div.tags
          (string/join
            " "
            (map #(str "#" (string/lower-case %))
                 (string/split (:tag_list @data) #" ")))]
         [:a.to-soundcloud {:href (:permalink_url @data)}
          "Go to SoundCloud"]]])]))

(defn text-detail-view [thing data]
  (r/with-let [_ (go (reset! data (:body (<! (http/get (:text thing))))))]
    [:div.detail.text
     [:div.content
      (when @data
        {:dangerouslySetInnerHTML
         {:__html (util/md->html @data)}})]]))

(defn dispatch-view [views think thing]
  (r/with-let [player (r/cursor think [:player])
               data   (r/cursor think [:data (:slug thing)])]
    [(get views (:type thing)) thing data player]))

(defn view [sin think view last]
  (r/with-let
    [thumbnail-views {"text"  text-thumbnail-view
                      "sound" sound-thumbnail-view}
     detail-views    {"text"  text-detail-view
                      "sound" sound-detail-view}

     _ (go (let [response (<! (http/get "/data/think.json"))
                 entries  (map #(assoc % :slug (util/to-slug (:title %)))
                               (:body response))]
             (swap! think assoc-in [:entries] (vec entries))))

     entries
     (r/track
       (fn []
         (let [{:keys [type entries]} @think]
           (if (nil? type)
             entries
             (vec (filter #(= type (:type %)) entries))))))

     slideshow
     (r/track
       (fn []
         {:route-item #(routes/think- {:id %})
          :route-back #(routes/think)
          :curr (match-get @view [:think id])
          :last (match-get @last [:think id])
          :entries @entries}))

     slideshow-element #(dispatch-view detail-views think %)

     player (r/cursor think [:player])

     icon-view
     (fn [think type]
       [:div.filter-button
        {:class (when (= type (:type @think)) "enabled")
         :on-click (fn [] (swap! think update-in [:type]
                                 #(if (= % type) nil type)))}
        [:div
         {:class (str "icon-" type)}
         (map (fn [id] ^{:key id}[:div.segment]) (range 5))]])]

    [:div#think-page.page
     [:div.title (sinusoid/hovered sin) "Think."]
     [:div.filters
      [icon-view think "sound"]
      [icon-view think "text"]
      (when false [icon-view think "pic"])]

     [deco/grid {:class "stuff"
                 :grid-size-em   16.25
                 :grid-margin-em  1.25}
      (for [thing @entries]
        ^{:key (:slug thing)}
        [dispatch-view thumbnail-views think thing])]

     [audio-player/object player]
     [slideshow/view slideshow slideshow-element]]))
