(ns irresponsible.oolong-test
  (#?(:clj :require :cljs :require-macros) [#?(:clj clojure.test :cljs cljs.test)
                                            :refer [deftest is]])
  (:require [irresponsible.oolong :as o]
            [irresponsible.oolong.util :as u]
            #?(:cljs [irresponsible.oolong.test.a])
            #?(:cljs [irresponsible.oolong.test.b]))
  #?(:clj (:import [clojure.lang ExceptionInfo])))

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

(with-redefs [com.stuartsierra.component/using id] ;; we aren't testing component...
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
