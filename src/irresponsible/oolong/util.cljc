(ns irresponsible.oolong.util
  (:require [com.stuartsierra.component :as cpt]))

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

(def qualisym?
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
   args: [sys-or-cpt deps]
   returns: sys-or-cpt with new dependency metadata"
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

(defn func-or-sym
  "Runs the function either provided or named by the provided symbol
   args: [form config]
   returns: whatever form returns
   throws: if invalid"
  [form config]
  (cond (fn? form)        (form config)
        (qualisym? form)  (run-symbol form config)
        :else (fatal "Expected function or qualified symbol" {:got form})))

;; ## Internal functions
;;
;; These functions deal with the brewing of components and systems
;; You shouldn't need to use these directly

(declare system simple-system any)

(defn system-map
  "Given a system map and a config map, brews the system described by the map
   args: [map config]
   returns: new system with any dependency metadata
   throws: if invalid"
  [form conf]
  (reduce-kv (fn [acc k v]
               (assoc acc k (any v (get conf k))))
             {} form))

(defn any-list
  "Brews a system or component from the list contained in the given context (ctx)
   List format:
   args: [form config]
     form: list of two or three items of the following forms:
       * (cpt subject opt-dep?)
       * (sys subject opt-dep?)
       * (sys {:system `map} opt-dep?)
       notes
         * these are lists (i.e. if you're not in an edn file, quote them)
         * the first item is a symbol
         * subject is either a qualified symbol or a function
         * opt-dep? will default to nil dependencies
   Returns: new system or component with any dependency metadata
   Throws: if invalid"
  [form config]
  (if (and (list? form)  (<= 2 (count form) 3)  ('#{cpt sys} (first form)))
    (let [[f1 f2 & [deps]] form]
      (using ((if (= 'cpt f1) func-or-sym system) f2 config) deps))
    (fatal "Expected a component or system list" {:got form})))

(defn simple-system
  "Takes a simplified system such as you might find at the top level
   args: [form config]
     form: a symbol, function or map
     config: config map
   returns: system
   throws: if invalid"
  [form config]
  (cond (fn? form)      (form config)
        (map? form)     (system-map form config)
        (symbol? form)  (run-symbol form config)
        :else
        (fatal "Expected a simple system form (ns-qualified symbol, fn, map)" {:got form})))
  
(defn system
  "Takes something that represents a system
   args: [form config]
     form: one of:
       * function: a system constructor function
       * map: a system map
       * list: a system list
       * ns-qualified symbol: names a function as above
     config: map of config data
   returns: system
   throws: if invalid"
  [form config]
  (let [err1 "Expected a system form (ns-qualified symbol, fn, map or list)"
        err2 "A system list must be 2 or 3 items in length"]
    (cond (fn? form)                  (form config)
          (map? form)                 (system-map form config)
          (qualisym? form)            (run-symbol form config)
          (not (list? form))          (fatal err1 {:got form})
          (not (<= 2 (count form) 3)) (fatal err2 {:got form})
          :else (let [[f1 f2 & [deps]] form]
                  (if (= 'sys f1)
                    (using (simple-system f2 config) deps)
                    (fatal err1 {:got form}))))))

(defn any
  "Takes anything valid!
   args: [form config]
     form: one of:
       * function: a component or system constructor function
       * map: a system map
       * list: a component or system list
       * ns-qualified symbol: names a function as above
     config: map of config data
   returns: depends on what you gave it
   throws: if invalid"
  [form config]
  (cond (fn? form)        (form config)
        (map? form)       (system-map form config)
        (list? form)      (any-list form config)
        (qualisym? form)  (run-symbol form config)
        :else
        (fatal "Expected a system form (symbol, fn, map or list)" {:got form})))
