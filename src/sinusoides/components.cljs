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

(ns sinusoides.components
  (:require [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.views.am :refer [am-view]]
            [sinusoides.views.do- :refer [do-view]]
            [sinusoides.views.main :refer [main-view]]
            [sinusoides.views.not-found :refer [not-found-view]]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.todo :refer [todo-view]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [reagent.core :as r]))

(defn root-view [app]
  (r/with-let [am  (r/cursor app [:am])
               do  (r/cursor app [:do])
               sin (r/cursor app [:sinusoid])]
    [:div#sinusoides
     [sinusoid/sinusoid-view :sinusoid-h app sin]
     [sinusoid/sinusoid-view :sinusoid-v app sin]
     [css-transitions {:transition-name "page"
                       :transition-appear true
                       :transition-appear-timeout 3000
                       :transition-enter-timeout 3000
                       :transition-leave-timeout 3000}
      (match [(:view @app)]
        [[:am]]    ^{:key :am-view}   [am-view sin am]
        [[:do]]    ^{:key :do-view}   [do-view sin do]
        [[:init]]  ^{:key :init-view} [:div]
        [[:main]]  ^{:key :main-view} [main-view sin]
        [[:todo]]  ^{:key :todo-view} [todo-view sin]
        :else      ^{:key :not-found-view} [not-found-view sin])]]))

(defn init-components! [state]
  (r/render-component
    [root-view state]
    (.getElementById js/document "components")))
