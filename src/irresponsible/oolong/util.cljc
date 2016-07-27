(ns irresponsible.oolong.util
  (:require [com.stuartsierra.component :as cpt]
   #?(:clj  [clojure.core.match :refer [match]]
      :cljs [cljs.core.match :refer-macros [match]])))

(defn fatal [message data]
  (-> (str "[FATAL] " message " " data)
      (ex-info data)
      throw))

#?(:cljs
   ;; clojure has this built in but it returns a var or throws.
   ;; we return an atom so deref works and nil inside if it is not found
   (defn find-var
     "Replacement for clojure function which returns a var by name or throws
      Returns an atom (whose content is nil on failure) so deref works
      args: [sym]
      returns: atom"
     [sym]
     (try (-> sym str (.replace "/" ".") (.replace "-" "_") js/eval atom)
        (catch js/Object e
          (fatal "Could not find var by symbol" {:got sym})))))

(def ns-qualified-sym?
  "True if provided arg is a namespace-qualified symbol"
  (every-pred symbol? namespace))

(defn load-symbol
  "Attempts to load a ns-qualified symbol by name (in clojure, requires the namespace too)
   args: [sym]
   returns: symbol
   throws: if symbol cannot be found or namespace cannot be loaded"
  [sym]
  (try
    (let [ns (-> sym namespace symbol)] ; symbol will fail if it's nil
      #?(:clj (require ns))
      @(find-var sym))
    (catch #?(:clj java.lang.Exception :cljs js/Object) e
      (fatal "Expected loadable symbol" {:got sym}))))

(defn using
  "Applies dependency metadata to the system or component
   - Allows users to provide a single keyword dep (makes a 1-vec)
   - Is tolerant of nil deps
   Args: [sys-or-cpt deps]
   Returns: sys-or-cpt with new dependency metadata"
  [sys-or-cpt deps]
  (if deps
    (cpt/using sys-or-cpt (if (keyword? deps) [deps] deps))
    sys-or-cpt))

(defn run-symbol
  "Loads a symbol and runs the function it names with the config
   To be useful, this function should return a component or system
   args: [form config]
   returns: what the function named by form returns
   throws: if the symbol cannot be found (in clj: also if the namespace cannot be loaded)"
  [form config]
  ((load-symbol form) config))

;; ## Internal functions
;;
;; These functions deal with the brewing of components and systems
;; You shouldn't need to use these directly

(declare system simple-system any)

(defn system-map
  "Given a system map and a config map, brews the system described by the map
   Args: [map config]
   Returns: new system with any dependency metadata"
  [form conf]
  (reduce-kv (fn [acc k v]
               (assoc acc k (any v (get conf k))))
             {} form))

(defn any-list
  "Brews a system or component from the list contained in the given context (ctx)
   List format:
     * (cpt qualified/symbol opt-dep?)
     * (sys qualified/symbol opt-dep?)
     * (sys {:system `map} opt-dep?)
     opt-dep? will default to nil dependencies
   Args: [form config]
   Returns: new system or component with any dependency metadata
   Throws: on malformed syntax"
  [form config]
  (match [form]
    [(['cpt cpt & deps] :seq)] (using (run-symbol cpt config) (first deps))
    [(['sys sys & deps] :seq)] (using (system sys config) (first deps))
    :else (fatal "Expected a component or system list" {:got form})))

(defn simple-system
  ""
  [form config]
  (cond (symbol? form) (run-symbol form config)
        (map? form)    (system-map form config)
        :else (fatal "Expected a simple system form (symbol or map)" {:got form})))
  
(defn system
  ""
  [form config]
  (match [form]
    [(s :guard symbol?)]       (run-symbol form config)
    [(s :guard map?)]          (system-map form config)
    [(['sys sys & deps] :seq)] (using (simple-system sys config) (first deps))
    :else (fatal "Expected a system form (symbol, map or list)" {:got form})))

(defn any
  ""
  [form config]
  (match [form]
    [(s :guard symbol?)] (run-symbol form config)
    [(s :guard map?)]    (system-map form config)
    [(s :guard list?)]   (any-list form config)
    :else (fatal "Expected a system form (symbol, map or list)" {:got form})))
  
