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
            [clojure.string :as string]
            [cljs.core.match :refer-macros [match]]
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
   :progress 0
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

               update-progress
               (fn []
                 (when (-> audio .-buffered .-length pos?)
                   (swap! state assoc-in [:progress] (-> audio
                                                         .-buffered
                                                         (.end 0)))))

               update-status
               (fn [status]
                 (prn "status changed:")
                 (prn "   - source: " @source)
                 (prn "   - status: " status)
                 (swap! state assoc-in [:status] status))]

           (reset!
             events
             [(events/listen audio "durationchange" update-time)
              (events/listen audio "timeupdate" update-time)
              (events/listen audio "progress" update-progress)
              (events/listen audio "play" #(update-status :play))
              (events/listen audio "playing" #(update-status :playing))
              (events/listen audio "pause" #(update-status :paused))
              (events/listen audio "ended" #(update-status :ended))
              (events/listen audio "error" #(update-status :error))
              (events/listen audio "abort" #(update-status :aborted))
              (events/listen audio "stalled" #(update-status :stalled))])

           (go-loop []
             (match [(<! ch)]
                    [:play]        (do (when (let [st (:status @state)]
                                               (or (= st :error)
                                                   (= st :aborted)))
                                         (.load audio))
                                       (.play audio) (recur))
                    [:pause]       (do (.pause audio) (recur))
                    [[:seek time]] (do (set! (.-currentTime audio) time) (recur))
                    :else nil))))

       :component-will-unmount
       (fn [this]
         (async/close! ch)
         (dorun (map events/unlistenByKey @events)))

       :reagent-render
       (fn []
         [:audio {:preload "none"
                  :src @source}])})))

(defn audio-player-view [source]
  (r/with-let [command-ch (async/chan)
               mouse-time (r/atom 0)
               state      (r/atom (audio-player-state source))

               is-playing
               #(let [st (:status @state)]
                  (or (= st :play)
                      (= st :playing)
                      (= st :stalled)))

               toggle-play
               #(go (>! command-ch (if (is-playing) :pause :play)))

               update-mouse-time
               #(reset! mouse-time (-> (.-clientX %)
                                       (- (.-left (.getBoundingClientRect (.-target %))))
                                       (- (.-clientLeft (.-target %)))
                                       (/ (.-offsetWidth (.-target %)))
                                       (* (:duration @state))))

               seek-time
               #(go (>! command-ch [:seek @mouse-time]))]

    [:div.audio-player {:class (clj->js (:status @state))}
     [:div.play-button
      {:class (when (is-playing) "is-playing")
       :on-click toggle-play}]

     [:div.seek-bar
      {:on-click seek-time
       :on-mouse-move update-mouse-time}

      (when (pos? @mouse-time)
        [:div.seek-bar-tooltip
         {:class
          (cond
            (> @mouse-time (:progress @state)) "unbuffered"
            (> @mouse-time (:current-time @state)) "buffered"
            :else "played")
          :style {:left (-> @mouse-time
                            (/ (:duration @state))
                            (* 100)
                            (str "%"))}}
         (str (quot @mouse-time 60) ":"
              (int (rem @mouse-time 60)))])

      (when (pos? (:progress @state))
        [:div.seek-bar-loaded
         {:style {:width (-> (:progress @state)
                             (/ (:duration @state))
                             (* 100)
                             (str "%"))}}])

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
