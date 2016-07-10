(ns irresponsible.oolong
  (:require [com.stuartsierra.component :as cpt]
            #?@(:clj [[clojure.tools.reader.edn :as edn]
                      [clojure.tools.reader.reader-types :as rt]])
            [irresponsible.oolong.util :refer [simple-system]]))

;; oolong is a simple config-based loader for stuartsierra's brilliant
;; `component` library that solves our dependency issues.

;; This file contains the high level functions for integrating oolong
;; into your application.

;; If you want to get going quickly, best check out the docs in the
;; README.md file at the root of the repository. You can also see it online
;; on github at https://github.com/jjl/oolong/

(defn brew
  "Given a configuration, brews the described system descriptor under the
   `:app` key using the entire file as configuration.
   Args: [config]
     - config: a map with an `:app` key which is a valid system descriptor
   Returns: new system with any dependencies resolved
   Throws: if system cannot be loaded"
  [{:keys [app] :as config}]
  (simple-system app config))

(def brew-master brew) ;; backcompat
;; For convenience, we alias a few things from `component`

#?(:clj
   (defn brew-file
    "Given a configuration file path, reads the file as edn and brews the
     described system descriptor under the `:app` key using the entire
     file as configuration.
     Args: [filename]
       - filename: a filename naming a file of edn which must take the form
                   of a map. The `:app` key in the map should point to a valid
                   RSD. The entire map will be used as configuration
     Returns: new system with any dependencies resolved
     Throws: if file does not exist, is invalid edn or is invalid oolong."
    [filename]
    (-> filename slurp rt/indexing-push-back-reader edn/read brew)))

(def Lifecycle "The Lifecycle protocol for components" cpt/Lifecycle)
(def start "Starts a component" cpt/start)
(def stop "Stops a component" cpt/stop)
(def start-system "Starts a system" cpt/start-system)
(def stop-system "Stops a system" cpt/stop-system)
