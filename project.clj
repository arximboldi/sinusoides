(defproject sinusoides "0.1.0-SNAPSHOT"
  :description "Web user interface for the Sinusoides app"
  :url "http://sinusoidesapp.appspot.com"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2657"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [secretary "1.2.1"]
                 [om "0.8.0-rc1"]
                 [prismatic/om-tools "0.3.10"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cljs-http "0.1.24"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]
                 [sablono "0.2.22"]]

  :plugins [[lein-cljsbuild "1.0.4"]]
  :source-paths ["src"]

  :cljsbuild {
    :builds [
      {:id "debug"
       :source-paths ["src"]
       :compiler {:output-to "build/debug/sinusoides.js"
                  :output-dir "build/debug/out"
                  :optimizations :none
                  :source-map true}}
      {:id "release"
       :source-paths ["src"]
       :compiler {:output-to "build/release/sinusoides.js"
                  :optimizations :advanced
                  :pretty-print true
                  :preamble ["react/react.min.js"]}}]})
