(ns sinusoides.core
  (:require [sinusoides.components :as components]
            [sinusoides.routes :as routes]
            [reagent.core :as r]))

(defonce app-state
  (r/atom
    {:view [:init]
     :do {:entries []
          :languages []
          :filter {:languages #{}}
          :detail nil}
     :am []}))

(defn init-app! []
  (enable-console-print!)
  (routes/init-router! app-state)
  (components/init-components! app-state))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! app-state update-in [:__figwheel_counter] inc))

(init-app!)
