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

(ns sinusoides.views.audio-player
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [sinusoides.util :as util]
            [clojure.string :as string]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :as async :refer [<! >!]]
            [reagent.core :as r]
            [goog.events :as events]))

(def state
  {:source nil
   :duration 0
   :current-time 0
   :progress 0
   :status nil})

(defn impl-view [state command-ch]
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
                 (prn "audio player: status changed")
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
              (events/listen audio "abort" #(update-status :aborted))])

           (go-loop []
             (match [(<! ch)]
                    [:play]        (do (when (let [st (:status @state)]
                                               (or (= st :error)
                                                   (= st :aborted)))
                                         (.load audio))
                                       (.play audio)
                                       (recur))
                    [:pause]       (do (.pause audio)
                                       (recur))
                    [:preload]     (do (set! (.-preload audio) true)
                                       (recur))
                    [[:seek time]] (do (set! (.-currentTime audio) time)
                                       (recur))
                    [nil]          nil
                    [bad-command]  (do (prn "audio player: bad command, "
                                            bad-command)
                                       (recur))))))

       :component-will-unmount
       (fn [this]
         (async/close! ch)
         (dorun (map events/unlistenByKey @events)))

       :reagent-render
       (fn []
         [:audio {:preload "none"
                  :src @source}])})))

(defn view [source]
  (r/with-let
    [command-ch (async/chan)
     mouse-time (r/atom 0)
     state      (r/atom (merge state {:source source}))

     is-playing
     #(let [st (:status @state)]
        (or (= st :play)
            (= st :playing)))

     is-started
     #(pos? (:current-time @state))

     toggle-play
     #(go (>! command-ch (if (is-playing) :pause :play)))

     seek-time
     #(go (>! command-ch [:seek @mouse-time]))

     update-mouse-time
     (fn [ev]
       (reset! mouse-time
               (-> (.-clientX ev)
                   (- (.-left (.getBoundingClientRect (.-target ev))))
                   (- (.-clientLeft (.-target ev)))
                   (/ (.-offsetWidth (.-target ev)))
                   (* (:duration @state))))
       (when (pos? (.-buttons ev))
         (seek-time)))

     enable-preload
     #(go (>! command-ch :preload))]

    [:div.audio-player {:class (str "state-" (clj->js (:status @state))
                                    (when (is-playing) " is-playing")
                                    (when (is-started) " is-started"))}
     [:div.play-button
      {:on-click toggle-play}]

     [:div.seek-bar
      {:on-mouse-down seek-time
       :on-mouse-over enable-preload
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

      [:div.seek-bar-range
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
                              (str "%"))}}])]]

     [impl-view state command-ch]]))
