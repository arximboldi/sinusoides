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

(defn state [command]
  {:command command
   :src nil
   :duration 0
   :current-time 0
   :progress 0
   :status nil})

(defn object [st]
  (let [events (atom)]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [audio (r/dom-node this)

               ensure-src
               #(when (not= % (.-src audio))
                  (set! (.-src audio) %)
                  (.load audio)
                  (swap! st assoc-in [:src] (.-src audio))
                  (swap! st assoc-in [:progress] 0))

               update-time
               (fn []
                 (swap! st assoc-in [:duration] (.-duration audio))
                 (swap! st assoc-in [:current-time] (.-currentTime audio)))

               update-progress
               (fn []
                 (when (-> audio .-buffered .-length pos?)
                   (swap! st assoc-in [:progress] (-> audio
                                                      .-buffered
                                                      (.end 0)))))

               update-status
               (fn [status]
                 (prn "audio player: status changed")
                 (prn "   - source: " (:src @st))
                 (prn "   - status: " status)
                 (swap! st assoc-in [:status] status))

               command-handler
               (fn [command]
                 (match [command]
                    [:pause]         (do (.pause audio))
                    [[:play src]]    (do (ensure-src src)
                                         (when (let [st (:status @st)]
                                                 (or (= st :error)
                                                     (= st :aborted)))
                                           (.load audio))
                                         (.play audio))
                    [[:preload src]] (do (ensure-src src)
                                         (set! (.-preload audio) true))
                    [[:seek time]]   (do (set! (.-currentTime audio) time))
                    [bad-command]    (do (prn "audio player: bad command, "
                                              bad-command))))]

           (reset! st (state command-handler))
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
              (events/listen audio "abort" #(update-status :aborted))])))

       :component-will-unmount
       (fn [this]
         (dorun (map events/unlistenByKey @events)))

       :reagent-render
       (fn []
         [:audio {:preload "none"}])})))

(defn view [props src st & children]
  (r/with-let
    [mouse-time (r/atom 0)

     is-src?
     #(= (:src @st) src)

     is-playing
     #(let [status (:status @st)]
        (and (is-src?)
             (or (= status :play)
                 (= status :playing))))

     is-started
     #(and (is-src?)
           (pos? (:current-time @st)))

     toggle-play
     #((:command @st) (if (is-playing)
                        :pause
                        [:play src]))

     seek-time
     #((:command @st) [:seek @mouse-time])

     update-mouse-time
     (fn [ev]
       (reset! mouse-time
               (-> (.-clientX ev)
                   (- (.-left (.getBoundingClientRect (.-target ev))))
                   (- (.-clientLeft (.-target ev)))
                   (/ (.-offsetWidth (.-target ev)))
                   (* (:duration @st))))
       (when (pos? (.-buttons ev))
         (seek-time)))

     enable-preload
     #((:command @st) [:preload src])]

    (into
      [:div.audio-player (r/merge-props
                           props
                           {:class (str "status-" (clj->js (:status @st))
                                        (when (is-playing) " is-playing")
                                        (when (is-started) " is-started"))})
       [:div.play-button
        {:on-click toggle-play}]

       [:div.seek-bar
        {:on-mouse-down seek-time
         :on-mouse-move update-mouse-time}

        (when (pos? @mouse-time)
          [:div.seek-bar-tooltip
           {:class
            (cond
              (> @mouse-time (:progress @st)) "unbuffered"
              (> @mouse-time (:current-time @st)) "buffered"
              :else "played")
            :style {:left (-> @mouse-time
                              (/ (:duration @st))
                              (* 100)
                              (str "%"))}}
           (str (quot @mouse-time 60) ":"
                (int (rem @mouse-time 60)))])

        [:div.seek-bar-range
         (when (pos? (:progress @st))
           [:div.seek-bar-loaded
            {:style {:width (-> (if (is-src?) (:progress @st) 0)
                                (/ (:duration @st))
                                (* 100)
                                (min 100)
                                (str "%"))}}])

         (when (pos? (:duration @st))
           [:div.seek-bar-position
            {:style {:width (-> (if (is-src?) (:current-time @st) 0)
                                (/ (:duration @st))
                                (* 100)
                                (min 100)
                                (str "%"))}}])]]]
      children)))
