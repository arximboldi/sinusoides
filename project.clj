(defproject sinusoides "0.1.0-SNAPSHOT"
  :description "Web user interface for the Sinusoides app"
  :url "http://sinusoidesapp.appspot.com"

  :dependencies [[clj-tagsoup "0.3.0"]
                 [cljs-http "0.1.39"]
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

  :min-lein-version "2.5.3"

  :plugins [[lein-figwheel "0.5.0-1"]
            [lein-cljsbuild "1.1.2"]]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/js/debug" "target"]

  :cljsbuild {
    :builds [
      {:id "debug"
       :source-paths ["src"]
       :figwheel {:on-jsload "sinusoides.core/on-figwheel-reload!"}
       :compiler {:asset-path "js/debug/out"
                  :output-to "resources/js/debug/sinusoides.js"
                  :output-dir "resources/js/debug/out"
                  :main sinusoides.core
                  :optimizations :none
                  :source-map-timestamp true}}
      {:id "release"
       :source-paths ["src"]
       :compiler {:asset-path "js/release/out"
                  :output-to "resources/js/release/sinusoides.js"
                  :output-dir "resources/js/release/out"
                  :main sinusoides.core
                  :optimizations :advanced
                  :pretty-print false}}]}

  :figwheel {:http-server-root ""
             :css-dirs ["resources/css"]})
