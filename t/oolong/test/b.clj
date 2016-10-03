(ns oolong.test.b
  (:use [irresponsible.oolong]))

(defrecord B [a activated]
  Lifecycle
  (start [{:keys [a activated] :as self}]
    (assoc-in self [:activated] :true))
  (stop [self]
    (-> self
        (assoc-in [:activated] nil)
        (assoc-in [:a] (stop a)))))

(defn cpt [args]
  (map->B args))
