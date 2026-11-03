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

(ns sinusoides.routes
  (:require [sinusoides.util :as util]
            [cljs.core.match]
            [clojure.string :as string]
            [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy])
  (:import goog.History))

(def history (atom))

(defn- page-node [[kind]]
  (js/document.getElementById (str (name kind) "-page")))

;; Scrolling happens inside #sinusoides rather than in the window, the
;; waveform scrolls along with the pages -- on /do it has to keep to the
;; line of the title -- and the page being left stays mounted for as long
;; as the transition lasts.  So the offset cannot simply be left where it
;; was: the page coming in sits at the top of that box, a screenful or
;; more above the window, and so does the waveform's resting place.  You
;; would see neither of them arrive.  They would turn up all at once at
;; the end, when the old page unmounts, the box shrinks back to one
;; screen and the browser clamps the offset to zero.
;;
;; The offset goes back to zero the moment the route changes instead, and
;; the pages left behind are pulled up by as much as they lose, so that
;; they carry on fading from where they were rather than jumping to their
;; own top.  They are moved with top and not with a transform, because the
;; titles and the slideshows are positioned fixed and a transform on an
;; ancestor would anchor them to the page instead of to the window.
;;
;; The page coming in is cleared rather than moved.  It is normally not
;; there yet, but a route can be left and taken again within the three
;; seconds, and React hands the same element back: it would otherwise
;; still be wearing the offset it was given on the way out.
;;
;; The waveform is given the offset back as well, and then, once the
;; browser has taken that as where it stands, asked for it again.  It is
;; the one thing here that is meant to move, so it travels the distance
;; over the length of the transition rather than crossing it in a frame:
;; .sinusoid-container carries the transition that does it.
;;
;; Which is why the giving back turns that transition off while it
;; happens.  It has to land in the frame it is asked for -- the box loses
;; the offset in that same frame, and anything the waveform has not been
;; told by then it jumps.  The transition would spread it over three
;; seconds instead, which is the whole of the jump and none of the
;; travel.  First time through it went unnoticed: top was still auto, and
;; auto cannot be interpolated, so it arrived at once of its own accord.
(defn- shift! [node offset]
  (set! (.. node -style -top) (str (- (.-offsetTop node) offset) "px")))

(defn- unscroll! [coming]
  (when-let [box (js/document.getElementById "sinusoides")]
    (let [offset (.-scrollTop box)
          pages  (.querySelectorAll box ".page, .fixed-page")
          waves  (.querySelectorAll box ".sinusoid-container")]
      (when (pos? offset)
        (dotimes [i (.-length pages)]
          (let [page (.item pages i)]
            (when-not (identical? page coming)
              (shift! page offset))))
        (dotimes [i (.-length waves)]
          (let [wave (.item waves i)]
            (.setProperty (.-style wave) "transition" "none")
            (shift! wave offset)))
        (set! (.-scrollTop box) 0)
        ;; Asking for a measurement settles everything above into the
        ;; place the waveform is to start from.  Left out, the browser
        ;; only ever sees where it ends up, and there is nothing to
        ;; transition from.
        (.getBoundingClientRect box)
        (dotimes [i (.-length waves)]
          (let [wave (.item waves i)]
            (.removeProperty (.-style wave) "transition")
            (set! (.. wave -style -top) "0px")))))
    (when coming
      (set! (.. coming -style -top) ""))))

(defn goto! [app page]
  (let [previous (:view @app)]
    ;; /think and /think/:id, /do and /do/:id, are one page with a detail
    ;; laid over it, and keep the place they were being read from.
    (when-not (= (first previous) (first page))
      (unscroll! (page-node page)))
    (swap! app assoc-in [:last] previous)
    (swap! app assoc-in [:view] page)))

(defn make-routes! [app]
  (defroute am "/am" []
    (goto! app [:am]))
  (defroute do "/do" []
    (goto! app [:do nil]))
  (defroute do- "/do/:id" [id]
    (goto! app [:do id]))
  (defroute think "/think" []
    (goto! app [:think nil]))
  (defroute think- "/think/:id" [id]
    (goto! app [:think id]))
  (defroute todo "/todo" []
    (goto! app [:todo]))
  (defroute main "/" []
    (goto! app [:main]))
  (defroute not-found "/dead-end" []
    (goto! app [:not-found])))

(defn- no-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (-> uri
       (string/replace (re-pattern (str "^" prefix)) "")
       (string/replace (re-pattern #"\?.*$") ""))))

(defn init-history! []
  (when (aget js/window "SINUSOIDES_DEBUG_MODE")
    (print "SINUSOIDES_DEBUG_MODE enabled")
    (secretary/set-config! :prefix "/debug"))
  (let [last-dispatched (atom)
        dispatch (fn [uri]
                   (reset! last-dispatched uri)
                   (secretary/dispatch! uri))
        match-uri (fn [uri]
                    (cond
                      ;; We avoid dispatching the same URI twice.
                      ;; This allows using fragments
                      ;; in an old-school way.
                      (and (not= @last-dispatched uri)
                           (secretary/locate-route (no-prefix uri)))
                      uri
                      ;; If not route is found and we just loaded the
                      ;; page, it basically means 404
                      (nil? @last-dispatched)
                      (not-found)))]
    (swap! history #(pushy/push-state! dispatch match-uri))))

(defn init! [state]
  (make-routes! state)
  (init-history!))

(defn nav! [token]
  (pushy/set-token! @history token))
