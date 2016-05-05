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

(ns sinusoides.views.main
  (:require [sinusoides.routes :as routes]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.addons :refer [css-transitions]]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn main-view [sin]
  (r/with-let [gens [#(rand-nth ["What "
                                 "Who "
                                 "Where "
                                 "Why "])
                     #(rand-nth [" I"
                                 " you"
                                 " they"])
                     #(rand-nth [[" I " "am"]
                                 [" you " "are"]
                                 [" they " "are"]])]
               parts (r/atom ;; (vec (map apply gens))
                       ["What " " you" [" I " "am"]])
               randomize (fn []
                           (let [[idx gen] (rand-nth (map-indexed vector gens))]
                             (swap! parts #(assoc % idx (gen)))))
               interval (.setInterval js/window randomize 5000)]
    [:div#main-page.page
     [:div#barcode]
     [:a#barcode2 {:href (routes/todo)}]
     [:div#main-text.links (sinusoid/hovered sin)
      [:div#main-pre-text (@parts 0)
       [:a {:href (routes/do)} "do"] (@parts 1)]
      [:div#main-post-text
       [:a {:href (routes/think)} "think"] ((@parts 2) 0)
       [:a {:href (routes/am)} ((@parts 2) 1)] "?"]]
     [:div#fingerprint.links
      [:a {:href (str "mailto:"
                   (s/reverse "gro.ung@vokinloksar"))}
       "CE3E CB30 6F40 3D98 DB2E" [:br]
       "B65C 529B A962 690A 70B1"]]]
    (finally (.clearInterval js/window interval))))
