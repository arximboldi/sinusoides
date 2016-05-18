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

(ns sinusoides.views.decorators
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [goog.events :as events]))

(defn focused [nested]
  (r/create-class
    {:component-did-mount #(.focus (r/dom-node %))
     :reagent-render      identity}))

(def animated
  (r/adapt-react-class
    js/React.addons.CSSTransitionGroup))

(defn sized [nested]
  (let [size
        (r/atom {:width 0 :height 0})

        update-size
        (fn [this]
          (let [node (r/dom-node this)]
            (reset! size {:width  (.-offsetWidth node)
                          :height (.-offsetHeight node)})))]

    (r/create-class
      {:component-will-mount
       (fn [this]
         (r/set-state
           this
           {:listener
            (events/listen js/window "resize"
                           #(update-size this))}))

       :component-did-mount
       update-size

       :component-will-unmount
       #(events/unlistenByKey (:listener (r/state %)))

       :reagent-render
       #(conj % size)})))
