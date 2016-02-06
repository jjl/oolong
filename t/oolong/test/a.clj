(ns oolong.test.a
  (:use [irresponsible.oolong]))

(defrecord A [a1 activated]
  Lifecycle
  (start [{:keys [a1 activated] :as self}]
    (assoc-in self [:activated] :true))
  (stop [self]
    (assoc-in self [:activated] nil)))

(defn cpt [args]
  (map->A args))
