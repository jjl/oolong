(defproject oolong "0.1.0-SNAPSHOT"
  :description "A config-based loader for stuartsierra's `component` library"
  :url "http://github.com/jjl/"
  :license {:name "MIT License"
            :url "https://en.wikipedia.org/wiki/MIT_License"}
  :plugins [[lein-midje "3.1.3"]
            [lein-marginalia "0.8.0"]
            [codox "0.8.11"]]
  :aliases {"docs" ["marg" "-f" "index.html"]}
  :source-paths ["src"]
  :test-paths ["t"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.reader "0.8.16"]
                 [com.stuartsierra/component "0.2.3"]
                 [midje "1.6.3"]]
;  :dev-dependencies [[midje "1.6.3"]]
  :profiles {:dev { :test-paths ["t"]}})

