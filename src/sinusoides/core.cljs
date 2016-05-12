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

(ns sinusoides.core
  (:require [sinusoides.components :as components]
            [sinusoides.routes :as routes]
            [reagent.core :as r]))

(defonce state
  (r/atom
    components/state))

(defn init! []
  (enable-console-print!)
  (routes/init! state)
  (components/init! state))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! state update-in [:__figwheel_counter] inc))

(init!)
