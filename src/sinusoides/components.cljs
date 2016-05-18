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
  (:require [sinusoides.views.decorators :as deco]
            [sinusoides.views.am :as am]
            [sinusoides.views.do- :as do-]
            [sinusoides.views.main :as main]
            [sinusoides.views.not-found :as not-found]
            [sinusoides.views.sinusoid :as sinusoid]
            [sinusoides.views.think :as think]
            [sinusoides.views.todo :as todo]
            [sinusoides.util :as util]
            [cljs.core.async :as async :refer [<!]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [clojure.string :as str]
            [reagent.core :as r]))

(def state
  {:view [:init]
   :last [:init]
   :loading true
   :fonts #{}
   :sinusoid sinusoid/state
   :do do-/state
   :think think/state
   :am am/state})

(defn view [app]
  (r/with-let [fonts (r/cursor app [:fonts])
               am    (r/cursor app [:am])
               do    (r/cursor app [:do])
               sin   (r/cursor app [:sinusoid])
               think (r/cursor app [:think])
               view  (r/track #(:view @app))
               last  (r/track #(:last @app))]
    [:div#sinusoides
     {:class (str/join " " (map #(str (name %) "-font-loaded") @fonts))}
     [sinusoid/view :sinusoid-h app sin]
     [sinusoid/view :sinusoid-v app sin]
     [deco/animated {:transition-name "page"
                     :transition-appear true
                     :transition-appear-timeout 3000
                     :transition-enter-timeout 3000
                     :transition-leave-timeout 3000}
      (match [@view]
        [[:am]]    ^{:key :am-view}   [am/view sin am]
        [[:do _]]  ^{:key :do-view}   [do-/view sin do view last]
        [[:init]]  ^{:key :init-view} [:div]
        [[:main]]  ^{:key :main-view} [main/view sin]
        [[:think _]] ^{:key :think-view} [think/view sin think view last]
        [[:todo]]  ^{:key :todo-view} [todo/view sin]
        :else      ^{:key :not-found-view} [not-found/view sin])]]))

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

(defn init! [state]
  (go
    (<! (load-preload!))
    (r/render [view state]
              (js/document.getElementById "components")
              #(load-delayed! state))))
