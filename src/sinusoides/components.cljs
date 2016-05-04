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
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sinusoides.util :as util]
            [sinusoides.routes :as routes]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [cljs.core.match :refer-macros [match]]
            [goog.events :as events]
            [cljs.core.match]
            [cljsjs.showdown]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn init-view []
  [:div "..."])

(def css-transitions
  (r/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn sinusoid-hover-clear [sin tag]
  (swap! sin #(update-in % [:hover] disj tag)))

(defn sinusoid-hover [sin tag & keyvals]
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

(defn sinusoid-hover? [sin]
  (-> @sin :hover empty? not))

(defn sinusoid-hovered [sin]
  {:class (when (sinusoid-hover? sin) "hovered")})

(defn not-found-view [sin]
  (r/with-let []
    [:div#not-found-page.page
     [:div#the-end (sinusoid-hovered sin)]
     [:a#dead-end {:href (routes/main)}
      [sinusoid-hover sin :not-found]
      [:span.first "Dead"] [:br]
      [:span.second "end"]]]
    (finally (sinusoid-hover-clear sin :not-found))))

(defn todo-view [sin]
  [:div#todo-page.page
   [:div#todo-block.links (sinusoid-hovered sin)
    "TO" [:a {:href (routes/do)} "DO."]]])

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
     [:div#main-text.links (sinusoid-hovered sin)
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

(defn md->html [str]
  (let [Converter (.-converter js/Showdown)
        converter (Converter.)]
    (.makeHtml converter str)))

(defn am-view [sin am]
  (r/with-let [updater (r/atom 1)
               should-add-children (r/atom false)
               _ (go (<! (timeout 2000))
                     (reset! should-add-children true))
               _ (go (let [response (<! (http/get "/data/am.json"))]
                       (reset! am (vec (shuffle (:body response))))))
               rand-px #(str (rand 60) "px")
               rand-ms #(str (rand %) "ms")]
    [:div#am-page.page
     [:div#am-block (sinusoid-hovered sin)
      [:div#i [:p " I " ]] [:br]
      [:div#am [:p" am "]] [:br]
      [:div#not [:p " not "]]]
     [:div#profiles.links
      (when (and @updater @should-add-children)
        [css-transitions {:transition-name "profile"
                          :transition-appear true
                          :transition-appear-timeout 3000
                          :transition-enter-timeout 3000}
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
               transition-name (str "swipe"
                                 (cond
                                   (< idx last-idx) "-left"
                                   (> idx last-idx) "-right"))]
           ^{:key :do-detail}
           [:div#do-detail.links
            [:div#left-side
             [css-transitions {:transition-name transition-name
                               :transition-enter-timeout 500
                               :transition-leave-timeout 500}
              ^{:key idx}
              [:a#img {:href (str "/static/screens/" ((:imgs p) 1))
                       :style {:background-image
                               (str "url(/static/screens/" ((:imgs p) 1) ")")}}]]]
            [:div#right-side
             [css-transitions {:transition-name transition-name
                               :transition-enter-timeout 500
                               :transition-leave-timeout 500}
              ^{:key idx}
              [:div#content
               [:div#header (:name p)]
               [:div#desc {:dangerouslySetInnerHTML
                           {:__html (md->html (:desc p))}}]
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
   [:div#presentation.links (sinusoid-hovered sin)
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
      [(filter-entries []
         (let [lang-filters (get-in @do [:filter :languages])]
           (vec (filter #(or (empty? lang-filters)
                           (contains? lang-filters (:lang %)))
                  (:entries @do)))))

       (find-entry [entries id]
         (when id
           (first (filter #(= id (:slug (% 1)))
                    (map-indexed vector entries)))))

       (fetch-data []
         (go (let [entries   (map #(assoc % :slug (util/to-slug (:name %)))
                               (:body  (<! (http/get "/data/do.json"))))
                   languages (apply sorted-set
                               (filter identity (map :lang entries)))]
               (swap! do assoc-in [:entries] entries)
               (swap! do assoc-in [:languages] languages))))]

    (fetch-data)
    (r/with-let [entries    (r/track filter-entries)
                 entry      (r/track #(find-entry @entries (:detail @do)))
                 last-entry (r/track #(find-entry @entries (:last @do)))]
      [do-view- sin do entries
       [do-detail-view entries entry last-entry]])))

(defn sinusoid-view [tag app sin]
  (let [expand #(match %
                  [:am]     ["am-sin" (routes/main)]
                  [:do]     ["do-sin" (routes/main)]
                  [:todo]   ["todo-sin" (routes/main)]
                  [:main]   ["main-sin" (routes/not-found)]
                  [:init]   ["init-sin" (routes/not-found)]
                  :else     ["not-found-sin" (routes/main)])
        [class1 _]    (expand (:last @app))
        [class2 href] (expand (:view @app))]
    [:a {:id tag
         :href href
         :class (str class1 "-last "
                  class2 " "
                  (when (sinusoid-hover? sin) "hovered"))}
     [sinusoid-hover sin tag]]))

(defn root-view [app]
  (r/with-let [am  (r/cursor app [:am])
               do  (r/cursor app [:do])
               sin (r/cursor app [:sinusoid])]
    [:div#sinusoides
     [sinusoid-view :sinusoid-h app sin]
     [sinusoid-view :sinusoid-v app sin]
     [css-transitions {:transition-name "page"
                       :transition-appear true
                       :transition-appear-timeout 3000
                       :transition-enter-timeout 3000
                       :transition-leave-timeout 3000}
      (match [(:view @app)]
        [[:am]]    ^{:key :am-view}   [am-view sin am]
        [[:do]]    ^{:key :do-view}   [do-view sin do]
        [[:init]]  ^{:key :init-view} [init-view]
        [[:main]]  ^{:key :main-view} [main-view sin]
        [[:todo]]  ^{:key :todo-view} [todo-view sin]
        :else      ^{:key :not-found-view} [not-found-view sin])]]))

(defn init-components! [state]
  (r/render-component
    [root-view state]
    (.getElementById js/document "components")))
