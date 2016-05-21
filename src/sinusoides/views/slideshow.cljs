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

(ns sinusoides.views.slideshow
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [sinusoides.views.decorators :as deco]
            [goog.events :as events]
            [reagent.core :as r]))

(def state
  {:entries []
   :curr nil
   :last nil
   :route-item (fn [slug])
   :route-back (fn [])})

(def prev-svg (util/embed-svg "resources/static/pic/left.svg"))
(def next-svg (util/embed-svg "resources/static/pic/right.svg"))
(def close-svg (util/embed-svg "resources/static/pic/close2.svg"))

(defn view [state item-view]
  (r/with-let
    [find-entry
     (fn [entries id]
       (when id
         (first (filter #(= id (:slug (% 1)))
                        (map-indexed vector entries)))))

     route-item (r/track #(:route-item @state))
     route-back (r/track #(:route-back @state))
     entries    (r/track #(:entries @state))
     entry      (r/track #(find-entry @entries (:curr @state)))
     last-entry (r/track #(find-entry @entries (:last @state)))

     nav-arrow!
     (fn [diff]
       (when-let [[idx _] @entry]
         (when-let [slug (get-in @entries [(+ idx diff) :slug])]
           (routes/nav! (@route-item slug)))))

     handle-key
     (fn [event]
       (case (.-keyCode event)
         37 (nav-arrow! -1)
         39 (nav-arrow! +1)
         27 (routes/nav! (@route-back))
         nil))

     listener (events/listen js/document "keydown" handle-key)]

    [deco/animated {:transition-name "slideshow"
                    :transition-appear true
                    :transition-appear-timeout 1000
                    :transition-enter-timeout 1000
                    :transition-leave-timeout 1000}
     (when @entry
       (let [[idx        item]  @entry
             [last-idx   _]     (or @last-entry [-1 nil])
             [transition z-idx]
             (cond
               (< idx last-idx) ["swipe-left"  (- (count @entries) idx)]
               (> idx last-idx) ["swipe-right" idx]
               true ["swipe" idx])]

         ^{:key :slideshow}
         [:div.slideshow
          [deco/animated {:transition-name transition
                          :transition-enter-timeout 500
                          :transition-leave-timeout 500}
           ^{:key idx}
           [deco/focused
            [:div.slideshow-item {:style {:z-index z-idx}
                                  :tab-index 1}
             [item-view item]]]]

          [:div.slideshow-controls
           (if-let [slug (get-in @entries [(- idx 1) :slug])]
             [:a.control.enabled {:href (@route-item slug)} prev-svg]
             [:div.control.disabled prev-svg])
           (if-let [slug (get-in @entries [(+ idx 1) :slug])]
             [:a.control.enabled {:href (@route-item slug)} next-svg]
             [:div.control.disabled next-svg])
           [:a.control.enabled {:href (@route-back)} close-svg]]]))]

    (finally (events/unlistenByKey listener))))
