(ns iokov.testing
  (:require [iokov.core :refer [iokov]]))

(defn flow
  "Executes iokov workflow with inital state and resolves output
  promise when goal has reached. Optionally accepts a list of new
  rules that may override the existing one in a workflow"
  ([workflow init-state goal & rules]
   (let [out (promise)]
     (->> workflow
          ;; Put new rules into workflow
          (map (fn[rule] (or (first (filter #(= (take 2 %) (take 2 rule)) rules))
                             rule)))
          ;; Disable all the rules which should run after goal has reached
          (map (fn[rule] (let [in (first rule)
                               disable #(cond-> % (= % goal) ::disabled)]
                           (apply vector (if (coll? in) (map disable in) (disable in))
                                  (rest rule)))))
          (#(conj % [:alts goal ::end (fn[r](deliver out (get r goal)))]))
          (apply iokov init-state))
     out)))
