(defproject irresponsible/oolong "0.4.0"
  :description "A config-based loader for stuartsierra's `component` library"
  :url "https://github.com/irresponsible/oolong/"
  :license {:name "MIT License"
            :url "https://en.wikipedia.org/wiki/MIT_License"
            :distribution :repo}
  :plugins [[lein-midje "3.2"]
            [codox "0.8.11"]]
  :source-paths ["src"]
  :test-paths ["t"]
  :clean-targets ^{:protect false} ["target"]
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.reader "1.0.0-alpha2"]
                 [irresponsible/tv100 "0.1.0"]
                 [com.stuartsierra/component "0.3.1"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}})


