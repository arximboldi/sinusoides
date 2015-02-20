(defproject sinusoides "0.1.0-SNAPSHOT"
  :description "Web user interface for the Sinusoides app"
  :url "http://sinusoidesapp.appspot.com"

  :dependencies [[cljs-http "0.1.24"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]
                 [kibu/pushy "0.2.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.10"]
                 [sablono "0.3.4"]
                 [secretary "1.2.1"]]

  :plugins [[lein-cljsbuild "1.0.4"]]
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
