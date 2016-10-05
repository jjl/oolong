(ns irresponsible.oolong-test
  (:use [midje.sweet])
  (:require [irresponsible.oolong :as o]
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

(facts :util
  (facts :general
    (fact :using
      (u/using 1 nil `identity) => 1
      (u/using 1 :1 `identity) => [:1]))
  (facts :internal
    (with-redefs [com.stuartsierra.component/using  id
                  ; return something different to distinguish
                  com.stuartsierra.component/system-using
                  #(apply concat (map vector % %2))]
      (facts :tvnqsym?
        (u/tvnqsym? 'sym) => (throws ExceptionInfo "Expected qualified symbol")
        (u/tvnqsym? `twice) => `twice)
      (facts :osym
        (u/osym 'nonexistent/symbol)
        => (throws ExceptionInfo "Expected loadable symbol")
        (u/osym 'twice)
        => (throws ExceptionInfo "Expected loadable symbol")
        ((u/osym `twice) 2) => 4)
      (facts :orun
        (u/orun {:form 'non/existent})
        => (throws ExceptionInfo "Expected loadable symbol")
        (u/orun {:form `twice :config 2}) => 4)
      (facts :osys-map
        (u/osys-map {:form {}}) => {}
        (u/osys-map {:form {:a `twice} :config {:a 2}}) => {:a 4})
      (facts :orsd
        (u/orsd {:form `identity :config :true}) => :true
        (u/orsd {:form {:a `twice} :config {:a 2}}) => {:a 4}
        (u/orsd {:form () :config :true})
        => (throws ExceptionInfo "Expected Reduced System Descriptor (map or symbol)"))
      (facts :osyslist
        (u/osyslist {:form ['cpt `identity] :config 123})
        => (throws ExceptionInfo "Expected sys")
        (u/osyslist {:form ['sys `identity] :config 123}) => 123
        (u/osyslist {:form ['sys `id :bar] :config  123})
        => [123 :bar]
        (u/osyslist {:form ['sys {:a `id}] :config {:a 1}}) => {:a '(1)})
      (facts :ocptlist
        (u/ocptlist {:form ['sys `identity] :config 123})
        => (throws ExceptionInfo "Expected cpt")
        (u/ocptlist {:form ['cpt `identity] :config 123}) => 123
        (u/ocptlist {:form ['cpt `id :bar] :config  123}))
      (facts :olist
        (u/olist {:form (list 'cpt `identity) :config 123}) => 123
        (u/olist {:form (list 'sys `identity) :config 123}) => 123
        (u/olist {:form (list 'sys {:a `id})  :config {:a 1}}) => {:a '(1)}
        (u/olist {:form (list 'sys ())})
        => (throws ExceptionInfo "Expected a component or system list")
        (u/olist {:form (list 'sys {})}) => {}
        (fact :dependencies
          (u/olist {:form (list 'cpt `id :bar) :config 123})
          => '((123) [:bar])
          (u/olist {:form (list 'sys `id :bar) :config 123})
          => [123 :bar]))
      (facts :ofsd
        (u/ofsd {:form `identity :config :true}) => :true
        (u/ofsd {:form {:a `twice} :config {:a 2}}) => {:a 4}
        (u/ofsd {:form (list 'cpt `identity) :config :true}) => :true))))

(facts :user-facing
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
    (fact :brew-master-file
      (derecordify (o/brew-master-file config-path)) => preactive)
    (fact :brew-master
      (derecordify master) => preactive)
    (fact :starting-and-stopping
      (let [s (o/start-system master)]
        (derecordify s) => active
        (derecordify (o/stop-system s)) => inactive))))
