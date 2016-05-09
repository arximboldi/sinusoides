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
  (:require [sinusoides.views.addons :refer [css-transitions]]
            [sinusoides.views.am :refer [am-view]]
            [sinusoides.views.do- :refer [do-view]]
            [sinusoides.views.main :refer [main-view]]
            [sinusoides.views.not-found :refer [not-found-view]]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.think :refer [think-view]]
            [sinusoides.views.todo :refer [todo-view]]
            [sinusoides.util :as util]
            [cljs.core.async :as async :refer [<!]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [clojure.string :as str]
            [reagent.core :as r]))

(defn root-view [app]
  (r/with-let [fonts (r/cursor app [:fonts])
               am    (r/cursor app [:am])
               do    (r/cursor app [:do])
               sin   (r/cursor app [:sinusoid])
               think (r/cursor app [:think])]
    [:div#sinusoides
     {:class (str/join " " (map #(str (name %) "-font-loaded") @fonts))}
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
        [[:think]] ^{:key :think-view} [think-view sin think]
        [[:todo]]  ^{:key :todo-view} [todo-view sin]
        :else      ^{:key :not-found-view} [not-found-view sin])]]))

(def preload-images
  ["/static/pic/barcode-v-i-r.svg"
   "/static/pic/sinusoid-i.svg"
   "/static/pic/sinusoid-i-v.svg"
   "/static/pic/left-i.svg"
   "/static/pic/right-i.svg"
   "/static/pic/close-i.svg"])

(def delayed-fonts
  {:main [{:family "Arimo"}
          {:family "Arimo" :weight "bold"}
          {:family "Arimo" :style "italic"}]
   :mono [{:family "Inconsolata"}
          {:family "Inconsolata" :style "bold"}]})

(defn load-preload! []
  (go
    (when-let [loading-node (js/document.getElementById "loading")]
      (<! (util/load-all util/load-image preload-images))
      (set! (.-className loading-node) "loaded")
      (go
        (<! (async/timeout 500))
        (.removeChild (.-parentNode loading-node) loading-node)))))

(defn load-delayed! [state]
  (go
    (let [loaded (r/cursor state [:fonts])
          load-font (fn [[name fonts]]
                      (go (<! (util/load-all util/load-font fonts))
                          (swap! loaded conj name)))]
      (<! (util/load-all load-font delayed-fonts)))))

(defn init-components! [state]
  (go
    (<! (load-preload!))
    (r/render [root-view state]
              (js/document.getElementById "components")
              #(load-delayed! state))))
