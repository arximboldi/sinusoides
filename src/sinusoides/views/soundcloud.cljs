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

;; Everything this site knows about SoundCloud.
;;
;; The pages used to talk to the v1 REST API: one call gave us the
;; track metadata and a `stream_url` we could hand to an <audio> tag.
;; That API is gone -- SoundCloud stopped handing out keys, revoked the
;; old ones, and streaming now needs an OAuth secret, which a static
;; site has nowhere to keep.  What is left without credentials is
;; oEmbed and the widget, so the players are built out of those:
;;
;;   - `info` gives the title and the artwork for a thumbnail, cheaply
;;     and in parallel, which is what the grid needs.
;;   - `sound` gives the description, the tags and the permalink, but
;;     costs a widget, so it is only worth it for the detail page.
;;   - `player-url` points an iframe at the player that actually makes
;;     the sound.  See views.audio-player.

(ns sinusoides.views.soundcloud
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<!]]))

(def ^:private api-url "https://w.soundcloud.com/player/api.js")

(def ^:private resolve-timeout-ms 15000)

(def ^:private api-script
  ;; Loaded on first use rather than from the page head: a visitor who
  ;; never opens Think never talks to SoundCloud at all.
  (delay
    (let [out    (async/promise-chan)
          script (.createElement js/document "script")]
      (set! (.-async script) true)
      (set! (.-onload script) #(async/put! out js/SC))
      ;; Content blockers reach for this script often enough that it is
      ;; worth saying so: callers get nil and can give up visibly,
      ;; rather than waiting on a channel that will never fill.
      (set! (.-onerror script) #(async/close! out))
      (set! (.-src script) api-url)
      (.appendChild (.-head js/document) script)
      out)))

(defn api-ready
  "Channel yielding js/SC once the widget API is usable, or nil if it
   could not be loaded at all."
  []
  @api-script)

(defn events
  "SC.Widget.Events.  Only valid once `api-ready` has produced a value."
  []
  (.. js/SC -Widget -Events))

(defn track-url [track]
  (str "https://api.soundcloud.com/tracks/" track))

(defn player-url
  "Wraps a track URL into the address of a player page, which is what an
   iframe's src wants.  Note the two are not interchangeable: once a
   frame holds a player, SC.Widget/load takes the bare track URL and
   builds its own player address from it.

   The player's own chrome is turned off -- ours is in
   _audio-player.scss -- and `auto-play` has to be part of the URL on
   the very first load, since by the time the iframe has booted the
   click that asked for it is long over."
  [url & [{:keys [auto-play]}]]
  (str "https://w.soundcloud.com/player/"
       "?url=" (js/encodeURIComponent url)
       "&auto_play=" (if auto-play "true" "false")
       "&show_artwork=false&show_comments=false&show_user=false"
       "&sharing=false&buying=false&download=false"))

(defn- strip-author
  "oEmbed titles arrive as \"Title by Author\"."
  [title author]
  (let [suffix (str " by " author)
        cut    (- (count title) (count suffix))]
    (if (and (pos? cut) (= suffix (subs title cut)))
      (subs title 0 cut)
      title)))

(defn info
  "Channel yielding {:title :artwork} for `track`, or nil.

   oEmbed is the one metadata endpoint left that needs no key, and it
   is CORS-open, so the whole grid can ask at once."
  [track]
  (go
    (let [url      (str "https://soundcloud.com/oembed?format=json&url="
                        (js/encodeURIComponent (track-url track)))
          response (<! (http/get url {:with-credentials? false}))
          body     (:body response)]
      (when (and (= 200 (:status response)) (:title body))
        {:title   (strip-author (:title body) (:author_name body))
         :artwork (:thumbnail_url body)}))))

(defn- parse-tags
  "SoundCloud packs tags into one string, quoting the ones that have
   spaces in them: `techno \"spoken word\" live`."
  [tag-list]
  (when-not (string/blank? tag-list)
    (->> (re-seq #"\"([^\"]*)\"|(\S+)" tag-list)
         (map #(or (nth % 1) (nth % 2)))
         (remove string/blank?)
         (vec))))

(defn- detach! [frame]
  (when (.-parentNode frame)
    (.removeChild (.-parentNode frame) frame)))

(defn sound
  "Channel yielding {:title :description :tags :permalink} for `track`,
   or nil if SoundCloud does not answer.

   oEmbed carries none of this, so we boot a throwaway player off-screen
   and ask it: `getCurrentSound` is the only way back to the old v1
   track fields that does not need a key."
  [track]
  (go
    (when-let [sc (<! (api-ready))]
      (let [out   (async/promise-chan)
            frame (.createElement js/document "iframe")]
        (.setAttribute frame "class" "audio-player-object")
        (.setAttribute frame "aria-hidden" "true")
        (.setAttribute frame "src" (player-url (track-url track)))
        (.appendChild (.-body js/document) frame)
        (let [w (.Widget sc frame)]
          (.bind w (.-READY (events))
                 (fn []
                   (.getCurrentSound
                     w (fn [js-sound]
                         (let [s (js->clj js-sound :keywordize-keys true)]
                           (async/put!
                             out
                             {:title       (:title s)
                              :description (:description s)
                              :tags        (parse-tags (:tag_list s))
                              :permalink   (:permalink_url s)})))))))
        (let [[result _] (async/alts! [out (async/timeout resolve-timeout-ms)])]
          (detach! frame)
          result)))))
