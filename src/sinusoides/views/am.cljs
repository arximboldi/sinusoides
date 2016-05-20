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

(ns sinusoides.views.am
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.decorators :as deco]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [reagent.core :as r]))

(def state [])

(defn view [sin am]
  (r/with-let [updater (r/atom 1)
               should-add-children (r/atom false)
               _ (go (<! (timeout 2000))
                     (reset! should-add-children true))
               _ (go (let [response (<! (http/get "/data/am.json"))]
                       (reset! am (vec (shuffle (:body response))))))
               rand-px #(str (rand 0.8) "em")
               rand-ms #(str (rand %) "ms")]
    [:div#am-page.page
     [:div#am-block (sinusoid/hovered sin)
      [:div#i [:p " I " ]] [:br]
      [:div#am [:p" am "]] [:br]
      [:div#not [:p " not "]]]
     [:div#profiles.links
      (when (and @updater @should-add-children)
        [deco/animated {:transition-name "profile"
                        :transition-appear true
                        :transition-appear-timeout 3000
                        :transition-enter-timeout 3000
                        :transition-leave-timeout 3000}
         (for [{name :name url :url} @am]
           ^{:key name}
           [:a
            {:id name
             :href url
             :on-mouse-over #(reset! updater name)
             :style {:padding-left (rand-px)
                     :padding-top (rand-px)
                     :animation-delay (rand-ms 1000)
                     :animation-duration (rand-ms 2000)}}
            [:span "this"]])])]]))
