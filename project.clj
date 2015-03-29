(defproject oolong "0.2.2"
  :description "A config-based loader for stuartsierra's `component` library"
  :url "http://github.com/jjl/"
  :license {:name "MIT License"
            :url "https://en.wikipedia.org/wiki/MIT_License"
            :distribution :repo}
  :plugins [[lein-midje "3.1.3"]
            [codox "0.8.11"]]
  :source-paths ["src"]
  :test-paths ["t"]
  :clean-targets ^{:protect false} ["target"]
  :deploy-repositories [["releases" :clojars]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["doc"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.reader "0.8.16"]
                 [com.stuartsierra/component "0.2.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}})


