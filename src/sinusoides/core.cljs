(ns sinusoides.core
  (:require [sinusoides.components :as components]
            [sinusoides.routes :as routes]))

(defn init-app! []
  (let [state (atom {:view [:init]})]
    (enable-console-print!)
    (routes/init-router! state)
    (components/init-components! state)))

(init-app!)
