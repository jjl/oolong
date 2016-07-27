(ns irresponsible.oolong-test
  (#?(:clj :require :cljs :require-macros) [#?(:clj clojure.test :cljs cljs.test)
                                            :refer [deftest is]])
  (:require [irresponsible.oolong :as o]
            [irresponsible.oolong.util :as u]
            #?(:clj [clojure.java.io :refer [resource]])
            #?(:clj [clojure.tools.reader.edn :as edn]
               :cljs [cljs.test :refer [do-report]])
            #?(:cljs [irresponsible.oolong.test.a])
            #?(:cljs [ irresponsible.oolong.test.b]))
  #?(:clj (:import [clojure.lang ExceptionInfo]
           [java.io StringWriter PrintWriter])))

#?(:cljs (enable-console-print!))

;; utilities for these tests

(def twice (partial * 2))
(def foo nil)
(defn id [& args] args)

(defn derecordify [n]
  (cond (vector? n) (mapv derecordify n)
        (list? n) (map derecordify n)

        ((some-fn map? record?) n)
        (reduce-kv (fn [acc k v]
                     (assoc acc
                            (derecordify k)
                            (derecordify v))) {} #?(:clj n :cljs (into {} n)))

        :else n))

;; cannot be arsed to figure out how to check the error message in cljs

#?(:clj (deftest fatal
          (is (= "[FATAL] foo {:bar :baz}"
                 (try (u/fatal "foo" {:bar :baz})
                      (catch ExceptionInfo e
                        (.getMessage e)))))))

(deftest ns-qualified-sym?
  (is (= true  (u/ns-qualified-sym? 'foo/bar)))
  (is (= false (u/ns-qualified-sym? 'foo)))
  (is (= false (u/ns-qualified-sym? 123)))
  (is (= false (u/ns-qualified-sym? "456")))
  (is (= false (u/ns-qualified-sym? [])))
  (is (= false (u/ns-qualified-sym? {})))
  (is (= false (u/ns-qualified-sym? :foo/bar))))

(deftest load-symbol
  (is (= ::caught
         (try (u/load-symbol 'nonexistent/symbol)
              (catch #?(:clj ExceptionInfo :cljs js/Object) e
                ::caught)))))

(deftest using
  (is (= {:a :b} (u/using {:a :b} nil)))
  (is (= {:a :b} (u/using {:a :b} :c))))

(with-redefs [com.stuartsierra.component/using id] ;; we aren't testing component...

 (deftest run-symbol
   (is (= ::caught
          (try (u/run-symbol 'non/existent 1)
               (catch #?(:clj ExceptionInfo :cljs js/Object) e
                 ::caught))))
   (is (= 4 (u/run-symbol `twice 2))))
  (deftest system-map
    (is (= {} (u/system-map {} {})))
    (is (= {:a 4} (u/system-map {:a `twice} {:a 2}))))
  (deftest simple-system
    (is (= :true (u/simple-system `identity :true)))
    (is (= (u/simple-system {:a `twice} {:a 2}) {:a 4}))
    (is (= ::caught
           (try (u/simple-system () :true)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               ::caught)))))
  (deftest any-list
    (is (= 123     (u/any-list (list 'sys `identity) 123)))
    (is (= {:a :b} (u/any-list (list 'sys `identity :bar) {:a :b})))
    (is (= {:a :b} (u/any-list (list 'sys {:a `identity}) {:a :b})))
    (is (= 123     (u/any-list (list 'cpt `identity) 123)))
    ;; we can't use a number because that can't hold metadata, a map can though.
    (is (= {:a :b} (u/any-list (list 'cpt `identity :bar) {:a :b})))
    (is (= ::caught
           (try (u/any-list '(sys ()) 1)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               ::caught)))))
  (deftest system
    (is (= ::true (u/system `identity ::true)))
    (is (= {:a 4} (u/system {:a `twice} {:a 2})))
    (is (= ::true (u/system (list 'sys `identity) ::true))))
  (let [config {:app {:a '(cpt irresponsible.oolong.test.a/cpt)
                      :b '(cpt irresponsible.oolong.test.b/cpt :a)}
                :a {:a1 :foo} :b {}}
        preactive {:a {:a1 :foo :activated nil}
                   :b {:a  nil  :activated nil}}
        inactive {:a {:a1 :foo :activated nil}
                  :b {:a  {:a1 :foo :activated nil}
                      :activated nil}}
        active {:a {:a1 :foo :activated :true}
                :b {:a {:a1 :foo :activated :true}
                    :activated :true}}]
    (deftest brewing
      (let [master (try (o/brew config)
                        (catch #?(:clj ExceptionInfo :cljs js/Object) e
                          (prn :fail-brewing (ex-data e) e)))]
        #?(:clj (is (= preactive (derecordify (o/brew-file "test/test.edn")))))
        (is (= preactive (derecordify master)))))
    (deftest start-stop
      (let [master (o/brew config)
            s (o/start-system master)]
        (is (= active (derecordify s)))
        (is (= inactive (derecordify (o/stop-system s))))))))
