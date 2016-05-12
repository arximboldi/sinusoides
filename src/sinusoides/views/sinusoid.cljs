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

(ns sinusoides.views.sinusoid
  (:require [sinusoides.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [reagent.core :as r]))

(def state {:hover #{}})

(defn hover-clear [sin tag]
  (swap! sin #(update-in % [:hover] disj tag)))

(defn hover-view [sin tag & keyvals]
  (r/with-let []
    [:div
     {:style {:width "100%"
              :height "100%"
              :position "absolute"
              :cursor "pointer"}
      :on-mouse-over
      (fn [] (swap! sin #(update-in % [:hover] conj tag)))
      :on-mouse-out
      (fn [] (swap! sin #(update-in % [:hover] disj tag)))}]
    (finally (swap! sin #(update-in % [:hover] disj tag)))))

(defn hover? [sin]
  (-> @sin :hover empty? not))

(defn hovered [sin]
  {:class (when (hover? sin) "hovered")})

(defn view [tag app sin]
  (let [expand #(match %
                  [:am]     ["am-sin" (routes/main)]
                  [:do _]   ["do-sin" (routes/main)]
                  [:init]   ["init-sin" (routes/not-found)]
                  [:main]   ["main-sin" (routes/not-found)]
                  [:think _]  ["think-sin" (routes/main)]
                  [:todo]   ["todo-sin" (routes/main)]
                  :else     ["not-found-sin" (routes/main)])
        [class1 _]    (expand (:last @app))
        [class2 href] (expand (:view @app))]
    [:div.sinusoid-container
     [:a {:id tag
          :href href
          :class (str class1 "-last "
                      class2 " "
                      (when (hover? sin) "hovered"))}
      [hover-view sin tag]]]))
