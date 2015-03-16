(ns oolong.test
  (:use [midje.sweet])
  (:require [oolong :as o]
            [oolong.util :as u]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :refer [resource]])
  (:import [clojure.lang ExceptionInfo]))

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

(facts :oolong
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
  (facts :util
    (facts :general
      ;; (fact :safely
      ;;   (macroexpand '(u/safely foo)) => (just `(try ~'foo (catch Exception ~'sym?))))
      (fact :always
        (u/always nil) => true)
      (fact :never
        (u/never true) => nil)
      (fact :rsd?
        (u/rsd? {}) => true
        (u/rsd? 'abc) => true
        (u/rsd? ()) => false
        (u/rsd? []) => false)
      (fact :fsd?
        (u/fsd? {}) => true
        (u/fsd? 'abc) => true
        (u/fsd? ()) => true
        (u/fsd? []) => false)
      (fact :load-sym
        (u/load-sym 'nonexistent/symbol) => nil
        (u/load-sym 'nonexistent/symbol true) => (throws ExceptionInfo "Could not load symbol")
        (with-redefs [foo :override]
          (u/load-sym `foo) => :override))
      (facts :expected
        (fact "formatting"
          (u/expected :foo :foo :foo) => (throws ExceptionInfo "Expected :foo"))
        (fact "info"
          (try (u/expected :foo :bar :baz)
            (catch ExceptionInfo e
              (let [i (ex-data e)]
                (fact i => {:expected :foo :got :bar :in :baz})))))
        (fact "merging"
          (try
            (u/expected :foo :bar :baz {:a :b})
            (catch ExceptionInfo e
              (let [i (ex-data e)]
                i => {:expected :foo :got :bar :in :baz :a :b})))))
      (fact :expects
        (u/expects u/never :foo :foo :foo) => (throws ExceptionInfo "Expected :foo")
        (u/expects u/always :foo :foo :foo) => nil)
      (fact :expecting
        (let [exp '(do (oolong.util/expects id :foo :foo :foo) bar)]
          (macroexpand '(u/expecting [id :foo :foo :foo] bar)) => exp))
      (fact :using
        (u/using 1 nil `identity) => 1
        (u/using 1 :1 `identity) => [:1]))
    (facts :internal
      (with-redefs [com.stuartsierra.component/using  id
                    ; return something different to distinguish
                    com.stuartsierra.component/system-using
                      #(apply concat (map vector % %2))]
        (facts :brew-cpt
          (fact :expectations
            (u/brew-cpt :b :true :true) => (throws ExceptionInfo "Expected namespace-qualified symbol in list"))
          (fact :identity
            (u/brew-cpt `identity :true :true) => :true))
        (facts :brew-map
          (u/brew-map {} {}) => {}
          (u/brew-map {:a `identity} {:a 1}) => {:a 1})
        (facts :brew-list
          (fact :simple
            (u/brew-list ['cpt `identity] 123) => 123
            (u/brew-list ['sys `identity] 123) => 123)
          (fact :dependencies
            (u/brew-list ['cpt `id :bar] 123) => '((123) [:bar])
            (u/brew-list ['sys `id :bar] 123) => [123 :bar])
          (fact :inline-maps
            (u/brew-list ['sys {:a `id}] {:a 1}) => {:a '(1)})
          (fact :exceptions
            (u/brew-list ['sys ()] 123) => (throws ExceptionInfo "Expected Reduced System Descriptor (map or symbol)")
            (u/brew-list ['baz `id] 123) => (throws ExceptionInfo "Expected symbol 'cpt or 'sys at head of list")))
        (facts :brew-sys
          (fact "When the app system is started, it will be provided with the entire configuration"
            (u/brew-sys `identity :true) => :true)
          (u/brew-sys (list 'cpt `identity) :true) => :true
          (u/brew-sys (list 'sys `id) :true) => '(:true)
          (u/brew-sys (list 'cpt 123) :true) => (throws ExceptionInfo "Expected namespace-qualified symbol in list"))))))
