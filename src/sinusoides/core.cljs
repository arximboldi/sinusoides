(ns sinusoides.core
  (:require [sinusoides.components :as components]
            [sinusoides.routes :as routes]))

(defn init-app! []
  (let [state (atom {:view [:init]
                     :do []
                     :am []})]
    (enable-console-print!)
    (routes/init-router! state)
    (components/init-components! state)))

(init-app!)
