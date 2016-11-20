(ns irresponsible.oolong-util-test
  (#?(:clj :require :cljs :require-macros) [#?(:clj clojure.test :cljs cljs.test)
                                            :refer [deftest is]])
  (:require [irresponsible.oolong.util :as u])
  #?(:clj (:import [clojure.lang ExceptionInfo])))

#?(:cljs (enable-console-print!))

;; utilities for these tests

(def twice (partial * 2))
(def foo nil)
(defn id [& args] args)

;; cannot be arsed to figure out how to check the error message in cljs

#?(:clj (deftest fatal
          (is (= "[FATAL] foo {:bar :baz}"
                 (try (u/fatal "foo" {:bar :baz})
                      (catch ExceptionInfo e
                        (.getMessage e)))))))

(deftest qualisym?
  (is (true? (u/qualisym? 'foo/bar)))
  (is (every? false? (map u/qualisym? ['foo 123 "456" [] {} :foo/bar]))))

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
    (is (= {:a 4} (u/system-map {:a `twice} {:a 2})))
    (is (= {:a 4} (u/system-map {:a twice} {:a 2}))))

  (deftest simple-system
    (is (= :true (u/simple-system `identity :true)))
    (is (= :true (u/simple-system identity :true)))
    (is (= (u/simple-system {:a `twice} {:a 2}) {:a 4}))
    (is (= (u/simple-system {:a twice} {:a 2}) {:a 4}))
    (is (= ::caught
           (try (u/simple-system () :true)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               ::caught)))))

  (deftest any-list
    (is (= 123     (u/any-list (list 'sys `identity) 123)))
    (is (= 123     (u/any-list (list 'sys identity) 123)))
    (is (= {:a :b} (u/any-list (list 'sys `identity :bar) {:a :b})))
    (is (= {:a :b} (u/any-list (list 'sys identity :bar) {:a :b})))
    (is (= {:a :b} (u/any-list (list 'sys {:a `identity}) {:a :b})))
    (is (= {:a :b} (u/any-list (list 'sys {:a identity}) {:a :b})))
    (is (= 123     (u/any-list (list 'cpt `identity) 123)))
    (is (= 123     (u/any-list (list 'cpt identity) 123)))
    ;; we can't use a number because that can't hold metadata, a map can though.
    (is (= {:a :b} (u/any-list (list 'cpt `identity :bar) {:a :b})))
    (is (= {:a :b} (u/any-list (list 'cpt identity :bar) {:a :b})))
    (is (= ::caught
           (try (u/any-list '(sys ()) 1)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               ::caught)))))

  (deftest system
    (is (= ::true (u/system `identity ::true)))
    (is (= ::true (u/system identity ::true)))
    (is (= {:a 4} (u/system {:a `twice} {:a 2})))
    (is (= {:a 4} (u/system {:a twice} {:a 2})))
    (is (= ::true (u/system (list 'sys `identity) ::true)))
    (is (= ::true (u/system (list 'sys identity) ::true))))
)
