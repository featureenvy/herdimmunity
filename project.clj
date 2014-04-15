(defproject herdimmunity "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.5.3"]]

  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-ring "0.8.10"]]

  :ring {:handler herdimmunity.server/app
         :auto-reload? true
         :auto-refresh? true
         :nrepl {:start? true :port 3001}}

  :source-paths ["src"]

  :cljsbuild {
  :builds [{:id "dev"
            :source-paths ["src"]
            :compiler {
              :output-to "resources/public/herdimmunity.js"
              :output-dir "resources/public/out"
              :optimizations :none
              :source-map true}}]})
