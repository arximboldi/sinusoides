(defproject sinusoides "0.1.0-SNAPSHOT"
  :description "Web user interface for the Sinusoides app"
  :url "http://sinusoidesapp.appspot.com"

  :dependencies [[cljs-http "0.1.39"]
                 [cljsjs/react-dom "0.14.3-1"]
                 [cljsjs/react-dom-server "0.14.3-0"]
                 [cljsjs/showdown "0.4.0-1"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]
                 [kibu/pushy "0.3.6"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [reagent "0.6.0-alpha"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.2"]]
  :source-paths ["src"]

  :cljsbuild {
    :builds [
      {:id "debug"
       :source-paths ["src"]
       :compiler {:output-to "build/debug/sinusoides.js"
                  :output-dir "build/debug/out"
                  :main sinusoides.core
                  :optimizations :none
                  :source-map true}}
      {:id "release"
       :source-paths ["src"]
       :compiler {:output-to "build/release/sinusoides.js"
                  :main sinusoides.core
                  :optimizations :advanced
                  :pretty-print false}}]})
