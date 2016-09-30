(ns oolong-test
  (:use [midje.sweet])
  (:require [tv100 :refer :all]
            [oolong :as o]
            [irresponsible.oolong.util :as u :refer :all]
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

(facts :util
  (facts :general
    (fact :using
      (using 1 nil `identity) => 1
      (using 1 :1 `identity) => [:1]))
  (facts :internal
    (with-redefs [com.stuartsierra.component/using  id
                  ; return something different to distinguish
                  com.stuartsierra.component/system-using
                  #(apply concat (map vector % %2))]
      (facts :tvnqsym?
        (tvnqsym? 'sym) => (throws ExceptionInfo "Expected qualified symbol")
        (tvnqsym? `twice) => `twice)
      (facts :tv->ctv
        ((tv->ctv tvtrue?) {:form true}) => {:form true}
        ((tv->ctv tvtrue? :baz) {:baz true}) => {:baz true})
      (facts :osym
        (osym 'nonexistent/symbol)
        => (throws ExceptionInfo "Expected loadable symbol")
        (osym 'twice)
        => (throws ExceptionInfo "Expected loadable symbol")
        ((osym `twice) 2) => 4)
      (facts :orun
        (orun {:form 'non/existent})
        => (throws ExceptionInfo "Expected loadable symbol")
        (orun {:form `twice :config 2}) => 4)
      (facts :osys-map
        (osys-map {:form {}}) => {}
        (osys-map {:form {:a `twice} :config {:a 2}}) => {:a 4})
      (facts :orsd
        (orsd {:form `identity :config :true}) => :true
        (orsd {:form {:a `twice} :config {:a 2}}) => {:a 4}
        (orsd {:form () :config :true})
        => (throws ExceptionInfo "Expected Reduced System Descriptor (map or symbol)"))
      (facts :osyslist
        (osyslist {:form ['cpt `identity] :config 123})
        => (throws ExceptionInfo "Expected sys")
        (osyslist {:form ['sys `identity] :config 123}) => 123
        (osyslist {:form ['sys `id :bar] :config  123})
        => [123 :bar]
        (osyslist {:form ['sys {:a `id}] :config {:a 1}}) => {:a '(1)})
      (facts :ocptlist
        (ocptlist {:form ['sys `identity] :config 123})
        => (throws ExceptionInfo "Expected cpt")
        (ocptlist {:form ['cpt `identity] :config 123}) => 123
        (ocptlist {:form ['cpt `id :bar] :config  123}))
      (facts :olist
        (olist {:form (list 'cpt `identity) :config 123}) => 123
        (olist {:form (list 'sys `identity) :config 123}) => 123
        (olist {:form (list 'sys {:a `id})  :config {:a 1}}) => {:a '(1)}
        (olist {:form (list 'sys ())})
        => (throws ExceptionInfo "Expected a component or system list")
        (olist {:form (list 'sys {})}) => {}
        (fact :dependencies
          (olist {:form (list 'cpt `id :bar) :config 123})
          => '((123) [:bar])
          (olist {:form (list 'sys `id :bar) :config 123})
          => [123 :bar]))
      (facts :ofsd
        (ofsd {:form `identity :config :true}) => :true
        (ofsd {:form {:a `twice} :config {:a 2}}) => {:a 4}
        (ofsd {:form (list 'cpt `identity) :config :true}) => :true))))

(facts :user-facing
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
    (fact :brew-master-file
      (derecordify (o/brew-master-file config-path)) => preactive)
    (fact :brew-master
      (derecordify master) => preactive)
    (fact :starting-and-stopping
      (let [s (o/start-system master)]
        (derecordify s) => active
        (derecordify (o/stop-system s)) => inactive))))
