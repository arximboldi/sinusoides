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
            [sinusoides.views.soundcloud :as soundcloud]
            [clojure.string :as string]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :as async :refer [<! >!]]
            [goog.object :as gobj]
            [reagent.core :as r]
            [goog.events :as events]))

(def ^:private load-timeout-ms 20000)

(defn- settled?
  "Waits for `ch`, giving up after a while.  A widget that never reports
   back would otherwise leave the command loop parked forever, and with
   it every player on the page."
  [ch]
  (go (let [[_ port] (async/alts! [ch (async/timeout load-timeout-ms)])]
        (= port ch))))

(defn state [command]
  {:command command
   :src nil
   :duration 0
   :current-time 0
   :progress 0
   :status nil})

(defn object [st]
  ;; One hidden SoundCloud widget, shared by every player on the page,
  ;; exactly as the single <audio> element it replaces was.  `src` is a
  ;; track URL now instead of a media URL, but nothing above here cares:
  ;; the view only ever compares it for identity, and every time and
  ;; duration below is still in seconds.
  (let [widget   (atom nil)
        commands (atom nil)
        ;; Kept apart from st: the widget reports how much it has
        ;; buffered as a fraction, so we cannot turn it into the
        ;; seconds the seek bar wants until we know the duration, and
        ;; the two do not arrive in a fixed order.
        loaded   (atom 0)
        ;; True from asking for a track until that track is in the
        ;; frame.  The widget posts its progress across from an iframe,
        ;; so the last word on the outgoing track lands after we have
        ;; already switched -- without this the new seek bar wears the
        ;; old track's position until its own first report.
        loading? (atom false)]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [frame (r/dom-node this)
               chan  (async/chan 16)

               sync!
               (fn [k v] (swap! st assoc-in [k] v))

               update-status
               (fn [status]
                 (prn "audio player: status changed")
                 (prn "   - source: " (:src @st))
                 (prn "   - status: " status)
                 (sync! :status status))

               update-progress
               (fn [] (sync! :progress (* @loaded (:duration @st))))

               update-duration
               (fn []
                 (when-let [w @widget]
                   (.getDuration w (fn [ms]
                                     (sync! :duration (/ ms 1000))
                                     (update-progress)))))

               bind!
               (fn [w]
                 (let [ev (soundcloud/events)]
                   (.bind w (.-PLAY ev)   #(update-status :playing))
                   (.bind w (.-PAUSE ev)  #(update-status :paused))
                   (.bind w (.-FINISH ev) #(update-status :ended))
                   (.bind w (.-ERROR ev)  #(update-status :error))
                   (.bind w (.-PLAY_PROGRESS ev)
                          (fn [e]
                            (when-not @loading?
                              (sync! :current-time
                                     (/ (gobj/get e "currentPosition")
                                        1000)))))
                   (.bind w (.-LOAD_PROGRESS ev)
                          (fn [e]
                            (when-not @loading?
                              (reset! loaded (gobj/get e "loadProgress"))
                              (update-progress))))))

               ;; Returns a channel that closes once the widget holds
               ;; `src`, so that the command loop below never has two
               ;; loads in flight.
               load!
               (fn [src auto-play]
                 (reset! loading? true)
                 (reset! loaded 0)
                 (swap! st merge {:src src
                                  :duration 0
                                  :current-time 0
                                  :progress 0
                                  :status nil})
                 (go
                   (let [ok
                         (if-let [w @widget]
                           ;; The frame already holds a player, so this
                           ;; takes the track URL itself -- handing it a
                           ;; player address would get that wrapped in a
                           ;; second one.
                           (let [done (async/promise-chan)]
                             ;; js-obj, not #js: the go macro cannot see
                             ;; into a reader literal, so a closure
                             ;; written there loses the locals around it.
                             (.load w src
                                    (js-obj "auto_play" auto-play
                                            "callback" #(async/close! done)))
                             (<! (settled? done)))
                           ;; Nothing has been played yet, so the iframe
                           ;; is still blank -- the equivalent of
                           ;; preload="none".
                           (if-let [sc (<! (soundcloud/api-ready))]
                             (let [ready (async/promise-chan)]
                               ;; auto-play has to ride in on the URL:
                               ;; the frame takes a moment to boot and by
                               ;; then the click that asked for it no
                               ;; longer counts as the gesture a browser
                               ;; wants to see.
                               (set! (.-src frame)
                                     (soundcloud/player-url
                                       src {:auto-play auto-play}))
                               (let [w (.Widget sc frame)]
                                 (reset! widget w)
                                 (bind! w)
                                 (.bind w (.-READY (soundcloud/events))
                                        #(async/close! ready)))
                               (if (<! (settled? ready))
                                 ;; If the browser refused the frame
                                 ;; anyway, ask again now that there is
                                 ;; a player to ask.
                                 (do (when auto-play (.play @widget))
                                     true)
                                 false))
                             false))]
                     ;; Whatever the track turned out to be, it is the
                     ;; one in the frame from here on, so its own
                     ;; reports are the ones worth listening to.
                     (reset! loading? false)
                     (if ok
                       (update-duration)
                       (update-status :error)))))]

           ;; React 0.14 drops attributes it does not know, and `allow`
           ;; is one of them -- without it the browser refuses the
           ;; iframe permission to make a sound.  It has to be in place
           ;; before the frame is ever navigated, which is why the src
           ;; is set lazily above and not in the render below.
           (.setAttribute frame "allow" "autoplay; encrypted-media")

           (reset! commands chan)
           (reset! st (state #(async/put! chan %)))

           (go-loop []
             (when-some [command (<! chan)]
               (match [command]
                  [:pause]         (when-let [w @widget] (.pause w))
                  [[:play src]]    (if (and @widget (= src (:src @st)))
                                     (.play @widget)
                                     (<! (load! src true)))
                  [[:preload src]] (when-not (= src (:src @st))
                                     (<! (load! src false)))
                  [[:seek time]]   (when-let [w @widget]
                                     (.seekTo w (js/Math.round (* time 1000))))
                  [bad-command]    (prn "audio player: bad command, "
                                        bad-command))
               (recur)))))

       :component-will-unmount
       (fn [this]
         (when-let [chan @commands] (async/close! chan))
         (when-let [w @widget]
           (.pause w)
           (let [ev (soundcloud/events)]
             (doseq [e [(.-READY ev) (.-PLAY ev) (.-PAUSE ev) (.-FINISH ev)
                        (.-ERROR ev) (.-PLAY_PROGRESS ev)
                        (.-LOAD_PROGRESS ev)]]
               (.unbind w e)))))

       :reagent-render
       (fn []
         [:iframe.audio-player-object
          {:scrolling "no" :title "SoundCloud player"}])})))

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
