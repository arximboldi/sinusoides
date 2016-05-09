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

(ns sinusoides.views.think
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.util :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [reagent.core :as r]))

(defn soundcloud-thumbnail-view [thing]
  [:iframe.thingy.soundcloud
   {:scrolling "no"
    :frame-border "no"
    :src (str "https://w.soundcloud.com/player/?"
              "url=https%3A//api.soundcloud.com/tracks/" (:track thing) "&amp;"
              "auto_play=false&amp;"
              "hide_related=true&amp;"
              "buying=false&amp;"
              "liking=false&amp;"
              "sharing=false&amp;"
              "download=false&amp;"
              "show_comments=false&amp;"
              "show_user=true&amp;"
              "show_reposts=false&amp;"
              "show_playcount=false&amp;"
              "visual=true")}])

(defn text-thumbnail-view [thing]
  [:div.thingy.text (:title thing)])

(def thumbnail-view-map
  {"soundcloud" soundcloud-thumbnail-view
   "text"       text-thumbnail-view})

(defn think-view [sin think]
  (r/with-let [_ (go (let [response (<! (http/get "/data/think.json"))
                           entries  (map #(assoc % :slug (util/to-slug (:title %)))
                                         (:body response))]
                       (swap! think assoc-in [:entries] entries)))]
    [:div#think-page.page
     [:div#title (sinusoid/hovered sin) "Think."]
     [:div#stuff
      (for [thing (:entries @think)]
        ^{:key (:slug thing)}
        [(get thumbnail-view-map (:type thing)) thing])]]))
