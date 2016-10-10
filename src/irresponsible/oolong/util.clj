(ns irresponsible.oolong.util
  (:use [tv100 :refer [tv-update tvsym? v->tv fail tvmap? tv-or tv=? tvlist?]])
  (:require [com.stuartsierra.component :as cpt]))

(defn fatal [message data]
  (-> (str "[FATAL] " message " " data)
      (ex-info data)
      throw))

;; ## Utility functions
;;
;; General purpose utility functions

(def tvnqsym?
  "tv-fn that expects a namespace-qualified symbol
   Args: [sym]
   Returns: input
   Throws: ExceptionInfo if not a namespace-qualified symbol"
  (comp tvsym?
        (v->tv "Expected qualified symbol" namespace)))

(def qualisym?
  "True if provided arg is a namespace-qualified symbol"
  (every-pred symbol? namespace))

(defn tv->ctv
  "Takes a tv-fn and turns it into a ctv-fn, acting on key (default: form)"
  ([tv-f]
     (tv->ctv tv-f :form))
  ([tv-f key]
     (tv-update tv-f key)))

(defn osym [sym]
  "Given a fully qualified symbol, loads its namespace and returns
   the value the symbol resolves to."
  (try
    (tvnqsym? sym)
    (-> sym namespace symbol require)
    @(find-var sym)
    (catch Exception e
      (fail "Expected loadable symbol" sym))))

(defn orun
  "Give a context with a symbol inside, runs it with the context config"
  [{:keys [form config]}]
  ((osym form) config))

(defn using
  "Applies the relevant `using-fn` to the tidied up `deps`
   - Allows users to provide a single keyword dep (makes a 1-vec)
   - Is tolerant of nil deps
   Args: [sys-or-cpt deps using-fn]
   Returns: sys-or-cpt with new dependency metadata"
  [sys-or-cpt deps using-fn]
  (if deps
    (using-fn sys-or-cpt (if (keyword? deps) [deps] deps))
    sys-or-cpt))

;; ## Internal functions
;;
;; These functions deal with the brewing of components and systems
;; You shouldn't need to use these directly

(declare orsd ofsd) ; declare ahead to solve recursive dependency

(def osys-map
  "Given a context map, brews the system described in the contained form
   Args: [ctx]
   Returns: new system of component with any dependency metadata"
  (comp (fn [{:keys [form config]}]
          (reduce-kv (fn [acc k v]
                       (assoc acc k (ofsd {:form v :config (config k)})))
                     {} form))
        (tv->ctv tvmap?)))

(def orsd
  "Matches a Reduced System Descriptor form"
  (tv-or "Expected Reduced System Descriptor (map or symbol)"
         osys-map orun))

(defn osyslist
  "Brews a system list with the config from the given context ctx
   sys-list is a 2-list or 3list: '(sys sym-or-map optional-dep-desc)
   Args: [ctx]
   Returns: new component
   Throws: if the symbol cannot be loaded or is not namespace qualified"
  [{:keys [form config]}]
  (let [[sym sys deps] form]
    ((tv=? 'sys) sym)
    (using (orsd {:config config :form sys})
           deps cpt/system-using)))

(defn ocptlist
  "Brews a component list with the config from the given context ctx
   cpt-list is a 2-list or 3list: '(cpt qualified/symbol optional-dep-desc)
   Args: [ctx]
   Returns: new component
   Throws: if the symbol cannot be loaded or is not namespace qualified"
  [{:keys [form config]}]
  (let [[sym cpt deps] form]
    ((tv=? 'cpt) sym)
    (using (orun {:config config :form cpt})
           deps cpt/using)))

(def olist
  "Brews a system or component from the list contained in the given context (ctx)
   List format:
     * (cpt qualified/symbol opt-dep?)
     * (sys qualified/symbol opt-dep?)
     * (sys {:system `map} opt-dep?)
     opt-dep? will default to nil dependencies
   Args: [ctx]
   Returns: new system or component with any dependency metadata
   Throws: on malformed syntax"
   (comp (tv-or "Expected a component or system list" ocptlist osyslist) (tv->ctv tvlist?)))

(def ofsd
  "Matches a Full System Descriptor form"
  (tv-or "Expected Full System Descriptor (FSD)"
         orsd olist))
