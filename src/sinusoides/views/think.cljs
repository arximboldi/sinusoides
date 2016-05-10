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
  (r/with-let
    [plyr-view
     ^{:component-did-mount
       (fn [this]
         (js/plyr.setup
           (r/dom-node this)
           (clj->js
             {:iconUrl "/static/pic/plyr-sprite.svg"
              :controls [:play :progress]
              :fullscreen {:enabled false}})))}
     (fn [data]
       (let [background (string/replace (:artwork_url @data)
                                        #"-large.jpg"
                                        "-t500x500.jpg")
             audio-src  (sc-add-client-id (:stream_url @data))]
         [:div.plyr
          {:style {:background-image (str "url(" background ")")}}
          [:audio {:src audio-src}]]))

     data
     (r/atom nil)
     _
     (go (let [response (<! (sc-api "/tracks/" (:track thing)))]
           (reset! data (:body response))))]

    [:div.thingy.soundcloud
     (when @data [plyr-view data])]))

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
