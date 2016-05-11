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
  (:require [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.util :as util]
            [clojure.string :as string]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >!]]
            [reagent.core :as r]
            [goog.events :as events]))

(def sc-client-id
  "485230fd2a6e151244a57a584f904070")

(defn sc-add-client-id [api-call]
  (str api-call "?client_id=" sc-client-id))

(defn sc-api [& command]
  (http/jsonp (sc-add-client-id
                (str "http://api.soundcloud.com"
                     (apply str command)
                     ".json"))))

(defn audio-player-state [source]
  {:source source
   :duration 0
   :current-time 0
   :progress nil
   :status nil})

(defn audio-player [state command-ch]
  (let [events (atom)
        source (r/track #(:source @state))
        ch     (->> (async/chan) (async/pipe command-ch))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [audio (r/dom-node this)

               update-time
               (fn []
                 (swap! state assoc-in [:duration] (.-duration audio))
                 (swap! state assoc-in [:current-time] (.-currentTime audio)))

               update-status
               (fn [status]
                 (swap! state assoc-in [:status] status))]

           (reset!
             events
             [(events/listen audio "durationchange" update-time)
              (events/listen audio "timeupdate" update-time)
              (events/listen audio "play" #(update-status :playing))
              (events/listen audio "playing" #(update-status :playing))
              (events/listen audio "pause" #(update-status :paused))
              (events/listen audio "ended" #(update-status :ended))
              (events/listen audio "error" #(update-status :error))])

           (go-loop []
             (case (<! ch)
               :play  (do (.play audio) (recur))
               :pause (do (.pause audio) (recur))
               nil))))

       :component-will-unmount
       (fn [this]
         (async/close! ch)
         (dorun (map events/unlistenByKey @events)))

       :reagent-render
       (fn []
         [:audio {:src @source}])})))

(defn audio-player-view [source]
  (r/with-let [command-ch (async/chan)
               state      (r/atom (audio-player-state source))]
    [:div.audio-player {:class (clj->js (:status @state))}
     [:div.play-button
      {:on-click #(go (>! command-ch
                          (if (= (:status @state) :playing)
                            :pause :play)))}]
     [:div.seek-bar
      (when (pos? (:duration @state))
        [:div.seek-bar-position
         {:style {:width (-> (:current-time @state)
                             (/ (:duration @state))
                             (* 100)
                             (str "%"))}}])]
     [audio-player state command-ch]]))

(defn soundcloud-thumbnail-view [thing]
  (r/with-let
    [data (r/atom nil)
     _    (go (let [response (<! (sc-api "/tracks/" (:track thing)))]
                (reset! data (:body response))))]
    (if @data
      (let [background (string/replace (:artwork_url @data)
                                       #"-large.jpg"
                                       "-t500x500.jpg")
            audio-src  (sc-add-client-id (:stream_url @data))]
        [:div.thingy.soundcloud
         {:style {:background-image (str "url(" background ")")}}
         [audio-player-view audio-src]])
      [:div.thingy.soundcloud])))

(defn text-thumbnail-view [thing]
  [:div.thingy.text (:title thing)])

(def thumbnail-view-map
  {"soundcloud" soundcloud-thumbnail-view
   "text"       text-thumbnail-view})

(defn think-view [sin think]
  (r/with-let [_ (go (let [response (<! (http/get "/data/think.json"))
                           entries  (map #(assoc % :slug (util/to-slug (:title %)))
                                         (:body response))]
                       (swap! think assoc-in [:entries] entries)))]
    [:div#think-page.page
     [:div#title (sinusoid/hovered sin) "Think."]
     [:div#stuff
      (for [thing (:entries @think)]
        ^{:key (:slug thing)}
        [(get thumbnail-view-map (:type thing)) thing])]]))
