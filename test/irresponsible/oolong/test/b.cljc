(ns irresponsible.oolong.test.b
  (:require [irresponsible.oolong :refer [Lifecycle start stop]]))

(defrecord B [a activated]
  Lifecycle
  (start [{:keys [a activated] :as self}]
    (assoc self :activated :true))
  (stop [self]
    (-> self
        (assoc :activated nil)
        (assoc :a (stop a)))))

(defn cpt [args]
  (map->B args))
