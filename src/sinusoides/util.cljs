(ns sinusoides.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [close! chan pipe <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn trace [obj & more]
  (apply prn (if more more ["TRACE:"]))
  (.dir js/console obj)
  obj)

(defn trace-debug [obj & more]
  (apply prn (if more more ["TRACE:"]))
  (.dir js/console obj)
  (js/eval "debugger")
  obj)

(defn to-slug [str]
  (-> str
    (.toLowerCase)
    (.replace "-+" "")
    (.replace "\\s+" "-")
    (.replace "[^a-z0-9-]" "")))

(defn chan-to-cursor-updates [cursor owner [input-channel korks]]
  (reify
    om/IInitState
    (init-state [_]
      (let [channel (pipe input-channel (chan))]
        (go-loop []
          (when-let [input (<! channel)]
            (om/update! cursor korks input)
            (recur)))
        {:channel channel}))

    om/IWillUnmount
    (will-unmount [_]
      (close! (om/get-state owner :channel)))

    om/IRender
    (render [_] (dom/div nil))))

(defn togglej [set thing]
  (if (contains? set thing)
    (disj set thing)
    (conj set thing)))
