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

(ns sinusoides.views.do-
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.addons :refer [css-transitions]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [goog.events :as events]
            [reagent.core :as r]))

(defn do-detail-view [entries entry last-entry]
  (letfn
      [(nav-arrow! [diff]
         (when-let [[idx _] @entry]
           (when-let [next (get-in @entries [(+ idx diff) :slug])]
             (routes/nav! (routes/do- {:id next})))))

       (handle-key [event]
         (case (.-keyCode event)
           37 (nav-arrow! -1)
           39 (nav-arrow! +1)
           27 (routes/nav! (routes/do))
           nil))]

    (r/with-let [listener (events/listen js/document "keydown" handle-key)]
      [css-transitions {:transition-name "do-detail"
                        :transition-appear true
                        :transition-appear-timeout 1000
                        :transition-enter-timeout 1000
                        :transition-leave-timeout 1000}
       (when @entry
         (let [[idx p] @entry
               last-idx (if @last-entry (@last-entry 0) -1)
               [transition-name z-index]
               (cond
                 (< idx last-idx) ["swipe-left"  (- (count @entries) idx)]
                 (> idx last-idx) ["swipe-right" idx])]
           ^{:key :do-detail}
           [:div#do-detail.links
            [:div#left-side
             [css-transitions {:transition-name transition-name
                               :transition-enter-timeout 500
                               :transition-leave-timeout 500}
              ^{:key idx}
              [:a#img {:href (str "/static/screens/" ((:imgs p) 1))
                       :style {:z-index z-index
                               :background-image
                               (str "url(/static/screens/" ((:imgs p) 1) ")")}}]]]
            [:div#right-side
             [css-transitions {:transition-name transition-name
                               :transition-enter-timeout 500
                               :transition-leave-timeout 500}
              ^{:key idx}
              [:div#content {:style {:z-index z-index}}
               [:div#header (:name p)]
               [:div#desc {:dangerouslySetInnerHTML
                           {:__html (util/md->html (:desc p))}}]
               (for [link (:link p)]
                 ^{:key link}
                 [:a.link {:href (:href link)} (:name link)])]]
             [:div#footer
              (if-let [id (get-in @entries [(- idx 1) :slug])]
                [:a.prev.enabled {:href (routes/do- {:id id})}]
                [:div.prev.disabled])
              (if-let [id (get-in @entries [(+ idx 1) :slug])]
                [:a.next.enabled {:href (routes/do- {:id id})}]
                [:div.next.disabled])
              [:a.close {:href (routes/do)}]]]]))]
      (finally (events/unlistenByKey listener)))))

(defn do-view- [sin do entries & children]
  [:div#do-page.page
   [:div#presentation.links (sinusoid/hovered sin)
    [:div.title "Do."]
    [:div.intro
     [:a {:href (routes/am)} "Being"]
     " is doing. One thing that I do a lot is building and talking
         about software. Most of it is "
     [:a {:href "http://www.gnu.org/philosophy/free-sw.html"}
      "libre software"]
     ". Libre software is a nice "
     [:a {:href (routes/think)} "thought"],
     " that blurs the boundaries between consumers and producers of
     software."
     [:em " Blah blah."]
     "You can taste a selection of my doing here."]]

   [:div#stuff
    [:div#language-links
     [:a.cv {:href "/static/files/resume-en.pdf"} "Résumé"]
     (doall
       (for [lang (:languages @do)]
         ^{:key lang}
         [:div.filter
          {:class (if (contains? (get-in @do [:filter :languages]) lang)
                    "on"
                    "off")
           :on-click
           (fn [] (swap! do update-in [:filter :languages]
                    #(util/togglej % lang)))}
          lang]))
     (when-not (empty? (get-in @do [:filter :languages]))
       [:div.filter.clearf
        {:on-click (fn [] (swap! do assoc-in [:filter :languages] #{}))}
        "Clear"])]

    [:div.programs
     (for [p @entries]
       ^{:key (:slug p)}
       [:a {:href (routes/do- {:id (:slug p)})
            :style {:background-image
                    (str "url(\"/static/screens/" ((:imgs p) 0) "\")")}}
        [:div]
        [:span (:name p)]])]]

   (first children)
   (rest children)])

(defn do-view [sin do]
  (letfn
      [(get-langs [p]
         (let [lang (:lang p)]
           (if (vector? lang)
             lang
             [lang])))

       (contain-any? [set v]
         (some #(contains? set %) v))

       (filter-entries []
         (let [lang-filters (get-in @do [:filter :languages])]
           (vec (filter #(or (empty? lang-filters)
                             (contain-any? lang-filters
                                           (get-langs %)))
                  (:entries @do)))))

       (find-entry [entries id]
         (when id
           (first (filter #(= id (:slug (% 1)))
                    (map-indexed vector entries)))))

       (fetch-data []
         (go (let [entries   (map #(assoc % :slug (util/to-slug (:name %)))
                               (:body  (<! (http/get "/data/do.json"))))
                   languages (apply sorted-set
                               (filter identity (mapcat get-langs entries)))]
               (swap! do assoc-in [:entries] entries)
               (swap! do assoc-in [:languages] languages))))]

    (fetch-data)
    (r/with-let [entries    (r/track filter-entries)
                 entry      (r/track #(find-entry @entries (:detail @do)))
                 last-entry (r/track #(find-entry @entries (:last @do)))]
      [do-view- sin do entries
       [do-detail-view entries entry last-entry]])))
