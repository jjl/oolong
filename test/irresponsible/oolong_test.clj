(ns irresponsible.oolong-test
  (:require [clojure.test :refer [deftest is]]
            [irresponsible.oolong :as o]
            [irresponsible.oolong.util :as u]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :refer [resource]])
  (:import [clojure.lang ExceptionInfo]
           [java.io StringWriter
                    PrintWriter]))

(def twice (partial * 2))
(def foo nil)
(defn id [& args] args)

(defn derecordify [n]
  (cond ((some-fn map? record?) n) (reduce-kv (fn [acc k v]
                                                 (assoc acc
                                                   (derecordify k)
                                                   (derecordify v))) {} n)
        (vector? n) (mapv derecordify n)
        (list? n) (map derecordify n)
        :else n))

(deftest fatal
  (is (= "[FATAL] foo {:bar :baz}"
         (try (u/fatal "foo" {:bar :baz})
              (catch ExceptionInfo e
                (.getMessage e))))))

(deftest qualisym?
  (is (true? (u/qualisym? 'foo/bar)))
  (is (every? false? (map u/qualisym? ['foo 123 "456" [] {} :foo/bar]))))

(deftest load-symbol
  (is (= ::caught
         (try (u/load-symbol 'nonexistent/symbol)
              (catch ExceptionInfo e
                ::caught)))))

(deftest using
  (is (= {:a :b} (u/using {:a :b} nil)))
  (is (= {:a :b} (u/using {:a :b} :c))))

(with-redefs [com.stuartsierra.component/using  id
              ; return something different to distinguish
              com.stuartsierra.component/system-using
              #(apply concat (map vector % %2))]

 (deftest run-symbol
   (is (= ::caught
          (try (u/run-symbol 'non/existent 1)
               (catch ExceptionInfo e
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
             (catch ExceptionInfo e
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
             (catch ExceptionInfo e
               ::caught)))))

  (deftest system
    (is (= ::true (u/system `identity ::true)))
    (is (= ::true (u/system identity ::true)))
    (is (= {:a 4} (u/system {:a `twice} {:a 2})))
    (is (= {:a 4} (u/system {:a twice} {:a 2})))
    (is (= ::true (u/system (list 'sys `identity) ::true)))
    (is (= ::true (u/system (list 'sys identity) ::true))))

  (let [config-path (resource "test.edn")
        config {:app {:a '(cpt irresponsible.oolong.test.a/cpt)
                      :b '(cpt irresponsible.oolong.test.b/cpt :a)}
                :a {:a1 :foo} :b {}}
        preactive {:a {:a1 :foo :activated nil}
                   :b {:a nil :activated nil}}
        inactive {:a {:a1 :foo :activated nil}
                  :b {:a {:a1 :foo :activated nil}
                      :activated nil}}
        active {:a {:a1 :foo :activated :true}
                :b {:a {:a1 :foo :activated :true}
                    :activated :true}}
        master (o/brew-master config)]

    (deftest brew-master-file
      (is (= (derecordify (o/brew-master-file config-path)) preactive))
    )
;    (deftest brew-master
;     (is (= (derecordify master) preactive))
;    )
;   (deftest start-stop
;      (let [s (o/start-system master)]
;        (is (= (derecordify s) active))
;        (is (= (derecordify (o/stop-system s)) inactive))
;      )
;    )
  )
)
