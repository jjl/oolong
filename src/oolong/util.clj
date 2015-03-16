(ns oolong.util
  (:require [com.stuartsierra.component :as cpt]))

;; ## Utility functions
;;
;; General purpose utility functions

(defmacro safely
  "A macro which wraps the nested exprs in a try
   In case of exception, the return value of the block will be nil"
  [& exprs]
  `(try ~@exprs
        (catch Exception e#)))

(def always
  "fn that always returns true. Used in tests"
  (constantly true))

(def never
  "fn that always returns nil. Used in tests"
  (constantly nil))

(def rsd?
  "Predicate that returns whether the provided form is a valid
   Reduced System Descriptor (RSD)
   Args: [form]"
  (some-fn map? symbol?))

(def fsd?
  "Predicate that returns whether the provided form is a valid
   Full System Descriptor (FSD)
   Args: [form]"
  (some-fn map? symbol? list?))

(defn load-sym
  "Given a fully qualified symbol, loads its namespace and returns
   the value the symbol resolves to.
   Given a symbol that does not resolve and the optional `fatal`
   argument, an exception will be thrown."
  [sym & [fatal]]
  (or (safely (-> sym namespace symbol require)
              @(find-var sym))
      (when fatal
        (throw (ex-info "Could not load symbol" {:sym sym})))))

(defn expected
  "Makes and throws an exception notifying that we got unexpected input
   Returns: never
   Args: [exp got in extra?]
     exp: what you expected. will be stringified into error message
     got: what you actually got
     in: the container you found the unexpected thing in
     extra: an optional map (default {}) of extra args to merge into
            the ex-info error information map"
  ([exp got in]
     (expected exp got in {}))
  ([exp got in extra]
     (throw (ex-info (str "Expected " exp)
                     (merge {:expected exp :got got :in in} extra)))))

(defn expects
  "If a predicate fails, make and throw an exception
   Args: [pred got exp in extra?]
     exp: what you expected. Will be stringified into error message
     in: the container you found the unexpected thing in
     extra: optional map of extra args to merge (per `expected` docstring)"
  ([p got exp in]
     (expects p got exp in {}))
  ([p got exp in extra]
     (when-not (p got)
       (expected exp got in extra))))

(defmacro expecting
  "Provides a cleaner way to enforce expectations.
   Args: [args & exprs]
     - args: the arguments you would provide `expect`, as a vector or list
     - exprs: the code you were going to write undeneath the `expect` call"
  [args & exprs]
  `(do (expects ~@args)
       ~@exprs))

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

(declare brew-sys) ; declare ahead to solve recursive dependency

(defn brew-cpt
  "Brews a component with the given config
   cpt-list is a 2-list or 3list: '(cpt qualified/symbol optional-dep-desc)
   Args: [config cpt-list]
   Returns: new component
   Throws: if the symbol cannot be loaded or is not namespace qualified"
  [sym list config]
  (expecting [symbol? sym "namespace-qualified symbol in list" list]
    ((load-sym sym true) config)))

(defn brew-map
  "Brews a system map with the given configuration
   Args: [sys config]
   Returns: new system of component with any dependency metadata"
  [sys config]
  (reduce-kv (fn [acc k v]
               (assoc acc k (brew-sys v (config k)))) {} sys))

(defn brew-list
  "Brews a system list with the given config
   Args: [list config]
     - list: a component or system list of one of the forms:
       * (cpt qualified/symbol opt-dep?)
       * (sys qualified/symbol opt-dep?)
       * (sys {:system `map} opt-dep?)
       opt-dep? will default to nil dependencies
     - config: the piece of configuration to initialise the cpt or sys with
   Returns: new system or component with any dependency metadata
   Throws: on malformed syntax"
  [[type desc & [deps] :as list] config]
  (expecting [#{'cpt 'sys} type "symbol 'cpt or 'sys at head of list" list]
    (if (= type 'cpt)
      (using (brew-cpt desc list config) deps cpt/using)
      (expecting [rsd? desc "Reduced System Descriptor (map or symbol)" list]
                 (using (brew-sys desc config) deps cpt/system-using)))))

(defn brew-sys
  "Brews the given system descriptor with the given config
   Args: [sys config]
   Returns: new system with any dependency metadata"
  [sys config]
  (expecting [fsd? sys "Full System Descriptor (map, list or symbol)" config]
    (cond (map? sys) (brew-map sys config)
          (list? sys) (brew-list sys config)
          (symbol? sys) ((load-sym sys true) config))))
