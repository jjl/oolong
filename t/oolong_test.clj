(ns oolong-test
  (:require [clojure.test :refer [deftest is]])
  (:require [tv100 :as t]
            [irresponsible.oolong :as o]
            [irresponsible.oolong.util :as u]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :refer [resource]])
  (:import [clojure.lang ExceptionInfo]
           [java.io StringWriter PrintWriter]))

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

(deftest using
  (is (= (u/using 1 nil `identity) 1))
  (is (= (u/using 1 :1 `identity) [:1])))

(with-redefs [com.stuartsierra.component/using  id
              ;; return something different to distinguish
              com.stuartsierra.component/system-using
              #(apply concat (map vector % %2))]

  (deftest tvnqsqm?
    (is (= (try
             (u/tvnqsym? 'sym)
             (catch Exception e ::caught))
           ::caught))
    (is (= (u/tvnqsym? `twice) `twice))
    (is (= ((u/tv->ctv t/tvtrue?) {:form true}) {:form true}))
    (is (= ((u/tv->ctv t/tvtrue? :baz)  {:baz true}))))

  (deftest osym
    (is (= (try
             (u/osym 'nonexistent/symbol)
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected loadable symbol"))
               ::caught)) ::caught))
    (is (= (try
             (u/osym 'nonexistent/symbol)
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected loadable symbol"))
               ::caught)) ::caught))
    (is (= ((u/osym `twice) 2) 4)))
  (deftest orun
    (is (= (try
             (u/orun {:form 'non/existent})
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected loadable symbol"))
               ::caught)) ::caught))
    (is (= (u/orun {:form `twice :config 2}) 4)))
  (deftest osys-map
    (is (= (u/osys-map {:form {}}) {}))
    (is (= (u/osys-map {:form {:a `twice} :config {:a 2}}) {:a 4})))
;;         => (throws ExceptionInfo "Expected Reduced System Descriptor (map or symbol)"))
  (deftest orsd
    (is (= (u/orsd {:form `identity :config :true}) :true))
    (is (= (u/orsd {:form {:a `twice} :config {:a 2}}) {:a 4}))
    (is (= (try
             (u/orsd {:form () :config :true})
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected Reduced System Descriptor (map or symbol)"))
               ::caught)) ::caught)))
  (deftest osyslist
    (is (= (try
             (u/osyslist {:form ['cpt `identity] :config 123})
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected sys"))
               ::caught)) ::caught))
    (is (= (u/osyslist {:form ['sys `identity] :config 123}) 123))
;    (is (= (u/osyslist {:form ['sys `id :bar] :config  123}) [123 :bar]))
    (is (= (u/osyslist {:form ['sys {:a `id}] :config {:a 1}}) {:a '(1)})))
  (deftest ocptlist    (is (= (try
             (u/ocptlist {:form ['sys `identity] :config 123})
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected cpt"))
               ::caught)) ::caught))
    (is (= (u/ocptlist {:form ['cpt `identity] :config 123}) 123))
;    (is (= (u/ocptlist {:form ['cpt `id :bar] :config  123}))
    )
  (deftest olist
    (is (= (u/olist {:form (list 'cpt `identity) :config 123}) 123))
    (is (= (u/olist {:form (list 'sys `identity) :config 123}) 123))
    (is (= (u/olist {:form (list 'sys {:a `id})  :config {:a 1}}) {:a '(1)}))
    (is (= (u/olist {:form (list 'sys {})}) {}))
    (is (= (try
             (u/olist {:form (list 'sys ())})
             (catch ExceptionInfo e
               (is (= (.getMessage e) "Expected a component or system list"))
               ::caught)) ::caught))
    (is (= (u/olist {:form (list 'cpt `id :bar) :config 123}) '((123) [:bar])))
    (is (= (u/olist {:form (list 'sys `id :bar) :config 123}) [123 :bar])))
  (deftest ofsd
    (is (= (u/ofsd {:form `identity :config :true}) :true))
    (is (= (u/ofsd {:form {:a `twice} :config {:a 2}}) {:a 4}))
    (is (= (u/ofsd {:form (list 'cpt `identity) :config :true}) :true)))
  (let [config-path (resource "test.edn")
        config {:app {:a '(cpt oolong.test.a/cpt)
                      :b '(cpt oolong.test.b/cpt :a)}
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
      (is (= (derecordify (o/brew-master-file config-path)) preactive)))
;;       
    (deftest brew-master
      (is (= (derecordify master) preactive)))
    (deftest start-stop
      (let [s (o/start-system master)]
        (derecordify s) => active
        (derecordify (o/stop-system s)) => inactive))))
