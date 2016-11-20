(ns irresponsible.oolong
  (:require [com.stuartsierra.component :as cpt]
            #?@(:clj [[clojure.tools.reader.edn :as edn]
                      [clojure.tools.reader.reader-types :as rt]])
            [irresponsible.oolong.util :refer [simple-system]]))

;; # irresponsible.oolong
;;
;; ## A simple config-based loader for stuartsierra's component library
;;
;; This file contains the high level functions for integrating oolong
;; into your application.
;;
;; If you want to get going quickly, best check out the docs in the
;; README.md file at the root of the repository. You can also see it online
;; on github at https://github.com/jjl/oolong/
;;
;; ## Cheatsheet
;;
;; ## Functions
;;
;; ### brew
;;
;; Takes a configuration containing a service description set and returns
;; a system with dependencies resolved.
;;
;;; (brew {:app  {:name '(spec )}
;;;        :name {nameconf      }})
(defn brew
  "Given a configuration, brews the described system descriptor under the
   `:app` key using the entire file as configuration.
   Args: [config]
     - config: a map with an `:app` key which is a valid system descriptor
   Returns: new system with any dependencies resolved
   Throws: if system cannot be loaded"
  [{:keys [app] :as config}]
  (simple-system app (dissoc config :app)))

(def ^:deprecated brew-master brew)       ; backcompat

;; ### brew-file
;;
;; **Not available in CLJS**
;;
;; A Convenience function that wraps `(brew conf)` by first loading the
;; passed filename as `edn`, and passes that as the configuration.
;;
;; ```clojure
;; (brew-file "path/to/file") ;; Mostly equivalent to
;;  (brew (edn/read (slurp "path/to/file")))
;; ```
;;
;;; (brew-file "path/to/file")
#?(:clj
 (defn brew-file
   "Given a configuration file path, reads the file as edn and brews the
    described system descriptor under the `:app` key using the entire
    file as configuration.
    Args: [filename]
      - filename: a filename naming a file of edn which must take the form
                  of a map. The `:app` key in the map should point to a valid
                  system descriptor. The entire map will be used as configuration
    Returns: new system with any dependencies resolved
    Throws: if file does not exist, is invalid edn or is invalid oolong."
   [filename]
   (-> filename slurp rt/indexing-push-back-reader edn/read brew)))
#?(:clj
 (def ^:deprecated brew-master-file brew-file))


;; ## com.stuartsierra.component functions
;;
;; For convenience, we alias a few things from
;; [`com.stuartsierra.component`](https://github.com/stuartsierra/component)
;; Note that clojurescript seems to take objection to doing this for Lifecycle
;; and its methods, so it's clj-only to make the cljs error more obvious.
;;
;; ### Lifecycle
;;
;; Alias for com.stuartsierra.component/Lifecycle
;; *Not available in CLJS*
;;; (Lifecycle ... )
;;
;; ### start
;;
;; Alias for com.stuartsierra.component/start
;; *Not available in CLJS*
;;; (start ... )
;;
;; ### stop
;;
;; Alias for com.stuartsierra.component/stop
;; *Not available in CLJS*
;;
;;; (stop ... )

#?(:clj (def Lifecycle "The Lifecycle protocol for components" cpt/Lifecycle))
#?(:clj (def start "Starts a component" cpt/start))
#?(:clj (def stop "Stops a component" cpt/stop))

;; ### start-system
;;
;; Alias for com.stuartsierra.component/start-system
;;; (start-system ... )
;;
;; ### stop-system
;;
;; Alias for com.stuartsierra.component/stop-system
;;; (stop-system ... )
(def start-system "Starts a system" cpt/start-system)
(def stop-system "Stops a system" cpt/stop-system)
