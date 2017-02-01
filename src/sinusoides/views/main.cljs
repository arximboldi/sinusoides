;; Copyright (c) 2011-2017 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
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
            [sinusoides.views.decorators :as deco]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn view [sin]
  (r/with-let
    [gens [#(rand-nth ["What "
                       "Who "
                       "Where "
                       "Why "])
           #(rand-nth [" I"
                       " you"
                       " they"])
           #(rand-nth [[" I " "am"]
                       [" you " "are"]
                       [" they " "are"]])]

     parts (r/atom ;; ["What " " you" [" I " "am"]]
             (vec (map apply gens)))

     update-parts
     (fn [parts]
       (let [[idx gen] (rand-nth (map-indexed vector gens))]
         (assoc parts idx (gen))))

     gen-new
     (fn [gen elem]
       (first (first
                (drop-while (fn [[a b]] (= a b))
                            (map vector (repeatedly gen) (repeat elem))))))

     randomize-question
     (fn []
       (let [[idx gen] (rand-nth (map-indexed vector gens))]
         (swap! parts #(gen-new (partial update-parts %) %))))

     interval   (.setInterval js/window randomize-question 6000)

     transition {:transition-name "part"
                 :transition-enter-timeout 2000
                 :transition-leave-timeout 2000}]

    [:div#main-page.fixed-page
     [:div.barcode]
     [:a.barcode2 {:href "https://sinusoid.al"}]

     [:div.main-text (sinusoid/hovered sin)
      [:div.parts
       [deco/animated transition
        ^{:key (@parts 0)}
        [:div.part (@parts 0) " " [:a {:href (routes/do)} "do"]]]]

      [:div.parts
       [deco/animated transition
        ^{:key (@parts 1)}
        [:div.part (@parts 1) " " [:a {:href (routes/think)} "think"]]]]

      [:div.parts
       [deco/animated transition
        ^{:key (@parts 2)}
        [:div.part ((@parts 2) 0)
         " " [:a {:href (routes/am)} ((@parts 2) 1)]
         [:span.questionmark "?"]]]]]

     [:div.fingerprint.links
      [:a {:href (str "mailto:"
                      (s/reverse "se.diosunis@epnauj"))}
       "CE3E CB30 6F40 3D98 DB2E" [:br]
       "B65C 529B A962 690A 70B1"]]]

    (finally (.clearInterval js/window interval))))
