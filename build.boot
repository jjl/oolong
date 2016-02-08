(set-env!
  :project 'irresponsible/oolong
  :version "0.0.1"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"                  :scope "provided"]
                  [org.clojure/tools.reader "1.0.0-alpha2"]
                  [irresponsible/tv100 "0.3.0-SNAPSHOT"]
                  [com.stuartsierra/component "0.3.1"]
                  [org.clojure/clojurescript "1.7.228"          :scope "test"]
                  [adzerk/boot-cljs "1.7.228-1"                 :scope "test"]
                  [adzerk/boot-test "1.1.0"                     :scope "test"]
                  [adzerk/boot-cljs-repl       "0.3.0"          :scope "test"]
                  [adzerk/boot-reload          "0.4.5"          :scope "test"]
                  [pandeiro/boot-http          "0.7.1-SNAPSHOT" :scope "test"]
                  [com.cemerick/piggieback     "0.2.1"          :scope "test"]
                  [weasel                      "0.7.0"          :scope "test"]
                  [org.clojure/tools.nrepl     "0.2.12"         :scope "test"]
                  [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]])

(import '[java.io File])
(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload    :refer [reload]]
         '[adzerk.boot-test :as t]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[pandeiro.boot-http :refer [serve]])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "A config-based loader for stuartsierra's `component` library"
       :url "https://github.com/irresponsible/oolong"
       :scm {:url "https://github.com/irresponsible/oolong.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  test-cljs {:js-env :phantom}
  target  {:dir #{"target"}})

(deftask clj-tests []
  (set-env! :source-paths #(conj % "test"))
  (comp (speak) (t/test)))

(deftask cljs-tests []
  (set-env! :source-paths #(conj % "test"))
  (comp (speak) (test-cljs)))

(deftask tests []
  (set-env! :source-paths #(conj % "test"))
  (comp (speak) (t/test) (test-cljs)))

(deftask autotest-clj []
  (set-env! :source-paths #(conj % "test"))
  (comp (watch) (speak) (t/test)))

(deftask autotest-cljs []
  (set-env! :source-paths #(conj % "test"))
  (comp (watch) (speak) (test-cljs)))

(deftask autotest []
  (comp (watch) (tests)))

(deftask dev-cljs []
  (set-env! :resource-paths #(-> % (conj "resources") (conj "test")))
  (comp (serve :dir "target/")
        (watch)
        (speak)
        (reload)
        (cljs-repl)
        (cljs :source-map true :optimizations :none)))

(deftask make-release-jar []
  (comp (pom) (jar)))

