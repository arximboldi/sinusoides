(ns sinusoides.core
  (:require [sinusoides.components :as components]
            [sinusoides.routes :as routes]
            [reagent.core :as r]))

(defn init-app! []
  (let [state (r/atom
                {:view [:init]
                 :do {:entries []
                      :languages []
                      :filter {:languages #{}}
                      :detail nil}
                 :am []})]
    (enable-console-print!)
    (routes/init-router! state)
    (components/init-components! state)))

(init-app!)
