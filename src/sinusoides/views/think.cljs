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
            [sinusoides.routes :as routes]
            [sinusoides.util :as util :refer-macros [match-get]]
            [clojure.string :as string]
            [cljs.core.match :refer-macros [match]]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >!]]
            [reagent.core :as r]
            [goog.events :as events]))

(def state
  {:entries []
   :data {}})

(def thumnail-views
  {"soundcloud"
   (fn [thing data]
     (r/with-let
       [client-id     "485230fd2a6e151244a57a584f904070"
        add-client-id #(str % "?client_id=" client-id)

        api-get
        (fn [& command]
          (http/jsonp (add-client-id (str "http://api.soundcloud.com"
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
           [:div.thingy.soundcloud
            {:style {:background-image (str "url(" background ")")}}
            [audio-player/view audio-src]])
         [:div.thingy.soundcloud])))

   "markdown"
   (fn [thing]
     [:a.thingy.text {:href (routes/think- {:id (:slug thing)})}
      (:title thing)])})

(defn thumbnail-view [think thing]
  (r/with-let [data (r/cursor think [:data (:slug thing)])]
    [(get thumnail-views (:type thing)) thing data]))

(def detail-views
  {"soundcloud"
   (fn [thing]
     [:div.detail.soundcloud "SOUNDCLOUD: " (:title thing)])

   "markdown"
   (fn [thing data]
     (r/with-let [_ (go (reset! data (:body (<! (http/get (:text thing))))))]
       [:div.detail.text
        [:div.content
         (when @data
           {:dangerouslySetInnerHTML
            {:__html (util/md->html @data)}})]]))})

(defn detail-view [think thing]
  (r/with-let [data (r/cursor think [:data (:slug thing)])]
    [(get detail-views (:type thing)) thing data]))

(defn view [sin think view last]
  (r/with-let
    [_ (go (let [response (<! (http/get "/data/think.json"))
                 entries  (map #(assoc % :slug (util/to-slug (:title %)))
                               (:body response))]
             (swap! think assoc-in [:entries] (vec entries))))

     slideshow
     (r/track
       (fn []
         {:route-item #(routes/think- {:id %})
          :route-back #(routes/think)
          :curr (match-get @view [:think id])
          :last (match-get @last [:think id])
          :entries (:entries @think)}))]
    [:div#think-page.page
     [:div#title (sinusoid/hovered sin) "Think."]
     [:div#stuff
      (for [thing (:entries @think)]
        ^{:key (:slug thing)}
        [thumbnail-view think thing])]
     [slideshow/view slideshow #(detail-view think %)]]))
