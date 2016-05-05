(ns irresponsible.oolong-test
  (#?(:clj :require :cljs :require-macros) [#?(:clj clojure.test :cljs cljs.test)
                                            :refer [deftest is]])
  (:require [irresponsible.tv100 :as t]
            [irresponsible.oolong :as o]
            [irresponsible.oolong.util :as u]
            #?(:clj [clojure.java.io :refer [resource]])
            #?(:clj [clojure.tools.reader.edn :as edn]
               :cljs [cljs.test :refer [do-report]]))
  #?(:clj (:import [clojure.lang ExceptionInfo]
           [java.io StringWriter PrintWriter])))

#?(:cljs (enable-console-print!))

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
  (is (= {:a :b} (u/using {:a :b} nil)))
  (is (= {:a :b} (u/using {:a :b} :c))))

(with-redefs [com.stuartsierra.component/using  id
              ;; return something different to distinguish
              com.stuartsierra.component/system-using
              #(apply concat (map vector % %2))]

  (deftest tvnqsqm?
    (is (= (try
             (u/tvnqsym? 'sym)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected qualified symbol")))
               ::caught))
           ::caught))
    (is (= (u/tvnqsym? `twice) `twice))
    (is (= ((u/tv->ctv t/tvtrue?) {:form true}) {:form true}))
    (is (= ((u/tv->ctv t/tvtrue? :baz)  {:baz true}) {:baz true})))
  (deftest osym
    #?(:cljs (is (= @(u/find-var `twice) twice)))
    (is (= (try
             (u/osym 'nonexistent/symbol)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected loadable symbol")))
               ::caught)) ::caught))
    (is (= (try
             (u/osym 'nonexistent/symbol)
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected loadable symbol")))
               ::caught)) ::caught))
    (is (= ((u/osym `twice) 2) 4)))
  (deftest orun
    (is (= (try
             (u/orun {:form 'non/existent})
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected loadable symbol")))
               ::caught)) ::caught))
    (is (= (u/orun {:form `twice :config 2}) 4)))
  (deftest osys-map
    (is (= (u/osys-map {:form {}}) {}))
    (is (= (u/osys-map {:form {:a `twice} :config {:a 2}}) {:a 4})))
  (deftest orsd
    (is (= (u/orsd {:form `identity :config :true}) :true))
    (is (= (u/orsd {:form {:a `twice} :config {:a 2}}) {:a 4}))
    (is (= (try
             (u/orsd {:form () :config :true})
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected Reduced System Descriptor (map or symbol)")))
               ::caught)) ::caught)))
  (deftest osyslist
    (is (= ::caught (try
             (u/osyslist {:form ['cpt `identity] :config 123})
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected sys")))
               ::caught))))
    (is (= 123 (u/osyslist {:form ['sys `identity] :config 123})))
    (is (= {:a :b} (u/osyslist {:form ['sys `identity :bar] :config {:a :b}}))))
    (is (= (u/osyslist {:form ['sys {:a `identity}] :config {:a :b}}) {:a :b}))
  (deftest ocptlist
    (is (= (try
             (u/ocptlist {:form ['sys `identity] :config 123})
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected cpt")))
               ::caught)) ::caught))
    (is (= 123 (u/ocptlist {:form ['cpt `identity] :config 123})))
    ;; we can't use a number because that can't hold metadata, a map can though.
    (is (= {:a :b} (u/ocptlist {:form ['cpt `identity :bar] :config {:a :b}}))))
  (deftest olist
    (is (= 123 (u/olist {:form (list 'cpt `identity) :config 123})))
    (is (= 123 (u/olist {:form (list 'sys `identity) :config 123})))
    (is (= {:a '(1)} (u/olist {:form (list 'sys {:a `id})  :config {:a 1}})))
    (is (= {} (u/olist {:form (list 'sys {})})))
    (is (= (try
             (u/olist {:form (list 'sys ())})
             (catch #?(:clj ExceptionInfo :cljs js/Object) e
               #?(:clj (is (= (.getMessage e) "Expected a component or system list")))
               ::caught)) ::caught))
    (is (= {:a :b} (u/olist {:form (list 'cpt `identity :bar) :config {:a :b}})))
    (is (= '({:a :b}) (u/olist {:form (list 'sys `id :bar) :config {:a :b}}) )))
  (deftest ofsd
    (is (= (u/ofsd {:form `identity :config :true}) :true))
    (is (= (u/ofsd {:form {:a `twice} :config {:a 2}}) {:a 4}))
    (is (= (u/ofsd {:form (list 'cpt `identity) :config :true}) :true)))
  (let [config {:app {:a '(cpt oolong.test.a/cpt)
                      :b '(cpt oolong.test.b/cpt :a)}
                :a {:a1 :foo} :b {}}
        preactive {:a {:a1 :foo :activated nil}
                   :b {:a nil :activated nil}}
        inactive {:a {:a1 :foo :activated nil}
                  :b {:a {:a1 :foo :activated nil}
                      :activated nil}}
        active {:a {:a1 :foo :activated :true}
                :b {:a {:a1 :foo :activated :true}
                    :activated :true}}]
    (deftest brewing
      (let [master (try (o/brew config)
                        (catch #?(:clj ExceptionInfo :cljs js/Object) e
                          (prn :fail-brewing (ex-data e))))]
        #?(:clj (is (= preactive (derecordify (o/brew-file "test/test.edn")))))
        (is (= preactive (derecordify master)))))))
    ;; (deftest start-stop
    ;;   (let [master (o/brew config)
    ;;         s (o/start-system master)]
    ;;     (is (= active (derecordify s)))
    ;;     (is (= inactive (derecordify (o/stop-system s))))))))
