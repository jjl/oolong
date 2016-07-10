(set-env!
  :project 'irresponsible/oolong
  :version "0.4.0"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha7"           :scope "provided"]
                  [org.clojure/tools.reader "1.0.0-alpha2"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [com.stuartsierra/component "0.3.1"]
                  [org.clojure/clojurescript "1.8.40"           :scope "test"]
                  [adzerk/boot-cljs "1.7.228-1"                 :scope "test"]
                  [adzerk/boot-test "1.1.0"                     :scope "test"]
                  [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-test :as t]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "A config-based loader for stuartsierra's `component` library"
       :url "https://github.com/irresponsible/oolong"
       :scm {:url "https://github.com/irresponsible/oolong.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  test-cljs {:js-env :phantom}
  target  {:dir #{"target"}})

(deftask testing []
  (set-env! :source-paths #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask clj-tests []
  (comp (testing) (speak) (t/test)))

(deftask cljs-tests []
  (comp (testing) (speak) (test-cljs)))

(deftask test []
  (comp (testing) (speak) (t/test) (test-cljs)))

(deftask autotest-clj []
  (comp (testing) (watch) (speak) (t/test)))

(deftask autotest-cljs []
  (comp (testing) (watch) (speak) (test-cljs)))

(deftask autotest []
  (comp (watch) (test)))

(deftask make-jar []
  (comp (pom) (jar)))

