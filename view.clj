(ns iokov.view
  (:require [loom.graph :refer [digraph weighted-digraph]]
            [loom.attr :refer [add-attr add-attr-to-nodes]]
            [loom.io :refer [view]]))

(defn apply-rule [graph rule]
  (let [[in out data] rule
        r-join #(-> %
                    (add-attr out :shape :component)
                    (add-attr out :style :filled)
                    (add-attr out :fillcolor :red))
        r-fork #(-> %
                    (add-attr in :shape :Mdiamond)
                    (add-attr in :style :filled)
                    (add-attr in :fillcolor :deepskyblue))]
    (cond-> graph
      (= :join (:type data)) r-join
      (= :fork (:type data)) r-fork
      (:duration data) (add-attr [in out] :label (:duration data)))))

(defn rule-data [rules]
  "Process iokov rules and generates list of rules like [input output {:type rule-type}]"
  (letfn [(edge [type in out]
            (cond
              (and (keyword? in) (keyword? out)) [(name in) (name out) {:type type}]
              (and (keyword? in) (coll? out)) (map (partial edge type in) out)
              (and (coll? in) (keyword? out)) (map #(edge type % out) in)))]
    (reduce #(if (seq? %2) (apply conj %1 (concat %2)) (conj %1 %2)) []
            (map #(edge (nth % 0) (nth % 1) (nth % 2)) rules))))

(defn build-graph [rule-data]
  (loop [graph (apply digraph (map (partial take 2) rule-data))
         rules rule-data]
    (if (not (seq rules))
         graph
         (recur (apply-rule graph (first rules)) (rest rules)))))

(defn render [rules]
  (-> rules rule-data build-graph view))
