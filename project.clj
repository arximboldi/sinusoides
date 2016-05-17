(defproject sinusoides "0.1.0-SNAPSHOT"
  :description "Web user interface for the Sinusoides app"
  :url "http://sinusoidesapp.appspot.com"

  :dependencies [[clj-tagsoup "0.3.0"]
                 [cljs-http "0.1.40"]
                 [cljsjs/react-with-addons "0.14.3-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cemerick/piggieback "0.2.1"]
                 [kibu/pushy "0.3.6"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [reagent "0.6.0-alpha2" :exclusions [cljsjs/react]]
                 [secretary "1.2.3"]]

  :min-lein-version "2.5.3"

  :plugins [[lein-figwheel "0.5.0-1"]
            [lein-cljsbuild "1.1.3"]
            [cider/cider-nrepl "0.13.0-SNAPSHOT"]]
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
                  :source-map-timestamp true
                  :parallel-build true}}
      {:id "release"
       :source-paths ["src"]
       :compiler {:asset-path "js/release/out"
                  :output-to "resources/js/release/sinusoides.js"
                  :output-dir "resources/js/release/out"
                  :main sinusoides.core
                  :optimizations :advanced
                  :pretty-print false
                  :parallel-build true}}]}

  :figwheel {:http-server-root ""
             :css-dirs ["resources/css"]
             :nrepl-port 7888
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                "cemerick.piggieback/wrap-cljs-repl"]})
