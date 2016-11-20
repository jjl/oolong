(set-env!
  :project 'irresponsible/oolong
  :version "0.4.0"
  :resource-paths #{"src" "resources"}
  :source-paths #{"src"}
  :description "A config-based loader for stuartsierra's `component` library"
  :url "https://github.com/irresponsible/oolong"
  :scm {:url "https://github.com/irresponsible/oolong"}
  :developers {"James Laver" "james@seriesofpipes.com"}
  :license {"MIT" "https://en.wikipedia.org/MIT_License"}
  :dependencies '[[org.clojure/clojure "1.8.0"           :scope "provided"]
                  [com.stuartsierra/component "0.3.1"]
                  [org.clojure/clojurescript "1.9.293"          :scope "test"]
                  [adzerk/boot-cljs "1.7.228-2"                 :scope "test"]
                  [adzerk/boot-test "1.1.2"                     :scope "test"]
                  [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-test :as t]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(task-options!
  pom {:url (get-env :url)
       :scm (get-env :scm)
       :project (get-env :project)
       :version (get-env :version)
       :license (get-env :license)
       :description (get-env :description)
       :developers (get-env :developers)}
  push {:tag            true
        :ensure-branch  "master"
        :ensure-release true
        :ensure-clean   true
        :gpg-sign       true
        :repo     "clojars"
        :repo-map [["clojars" {:url "https://clojars.org/repo/"}]]}
  test-cljs {:js-env :phantom}
  target  {:dir #{"target"}})

(deftask testing []
  (set-env! :source-paths #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask clj-tests []
  (comp (testing) (t/test)))

(deftask cljs-tests []
  (comp (testing) (test-cljs)))

(deftask test []
  (comp (testing) (t/test) (test-cljs)))

(deftask autotest-clj []
  (comp (testing) (watch) (speak) (t/test)))

(deftask autotest-cljs []
  (comp (testing) (watch) (speak) (test-cljs)))

(deftask autotest []
  (comp (watch) (test)))

;; RMG Only stuff
(deftask make-jar []
  (comp (pom) (jar) (target)))

(deftask release []
  (comp (pom) (jar) (push)))

;; Travis Only stuff
(deftask travis []
  (testing)
  (comp (t/test) (test-cljs)))

(deftask travis-installdeps []
  (testing) identity)
