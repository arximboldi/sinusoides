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
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.util :as util]
            [clojure.string :as string]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [reagent.core :as r]
            [plyr]))

(def sc-client-id
  "485230fd2a6e151244a57a584f904070")

(defn sc-add-client-id [api-call]
  (str api-call "?client_id=" sc-client-id))

(defn sc-api [& command]
  (http/jsonp (sc-add-client-id
                (str "http://api.soundcloud.com"
                     (apply str command)
                     ".json"))))

(defn plyr-sc-thumbnail-view [thing]
  (let [data (r/atom {})]
    ^{:component-did-mount
      (fn [this]
        (go (reset! data (:body (<! (sc-api "/tracks/" (:media-id thing)))))
            (.setup
              js/plyr
              (r/dom-node this)
              (clj->js
                {:iconUrl "/static/pic/plyr-sprite.svg"
                 :controls [:play :progress :current-time :duration]
                 :fullscreen {:enabled false}}))))}
    (fn [thing]
      (if @data
        (let [background (string/replace (:artwork_url @data)
                                         #"-large.jpg"
                                         "-t500x500.jpg")]
          [:div.thingy.soundcloud.plyr
           {:style {:background-image (str "url(" background ")")}}
           [:audio [:source {:src (sc-add-client-id (:stream_url @data))
                             :type "audio/mp3"}]]])
        [:div]))))

(def plyr-thumbnail-embed-view
  ^{:component-did-mount
    (fn [this]
      (js/plyr.setup (r/dom-node this)
        (clj->js
          {:iconUrl "/static/pic/plyr-sprite.svg"
           :controls [:play :progress :current-time :duration]
           :fullscreen {:enabled false}})))}

  (fn [thing]
    [:div.thingy.plyr
     [:div {:data-video-id (:media-id thing)
            :data-type (:type thing)}]]))

(defn text-thumbnail-view [thing]
  [:div.thingy.text (:title thing)])

(def thumbnail-view-map
  {"soundcloud" plyr-sc-thumbnail-view
   "text"       text-thumbnail-view})

(defn think-view [sin think]
  (r/with-let [_ (js/plyr.setup)
               _ (go (let [response (<! (http/get "/data/think.json"))
                           entries  (map #(assoc % :slug (util/to-slug (:title %)))
                                         (:body response))]
                       (swap! think assoc-in [:entries] entries)))]
    [:div#think-page.page
     [:div#title (sinusoid/hovered sin) "Think."]
     [:div#stuff
      (for [thing (:entries @think)]
        ^{:key (:slug thing)}
        [(get thumbnail-view-map (:type thing)) thing])]]))
