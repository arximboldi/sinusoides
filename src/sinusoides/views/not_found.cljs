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

(ns sinusoides.views.not-found
  (:require [sinusoides.routes :as routes]
            [sinusoides.views.sinusoid :as sinusoid]))

(defn view [sin]
  [:div#not-found-page.fixed-page
   [:div.the-end (sinusoid/hovered sin)]
   [:a.dead-end {:href (routes/main)}
    [sinusoid/hover-view sin :not-found]
    [:span.first "Dead"] [:br]
    [:span.second "end"]]])
