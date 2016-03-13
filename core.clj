(ns iokov.core)

(defn iokov [init-state & rules]
  (let [normalize (fn[idx](fn[rule](update-in rule [idx] #(if (coll? %) % (vector %)))))
        context (atom {:state init-state
                       :rules (map (comp (normalize 1) (normalize 2)) rules)})]
    (letfn [(find-rules []
              (filter #(if (= (first %) :join)
                         (every? true? (map (partial contains? (:state @context)) (second %)))
                         (seq (select-keys (:state @context) (second %))))
                      (:rules @context)))
            (exec-rules [rules]
              (let [call-handler (fn[in _ f] (f in))
                    rets (map (fn[[_ input output f & handlers :as rule]]
                                (let [rule-input (select-keys (:state @context) input)
                                      rule-result (map #(% rule-input output f input finish-rule) handlers)]
                                  (if (not-any? identity rule-result)
                                    ;; All of the handlers returned null, not an async job - call function directly
                                    [(call-handler rule-input output f)]
                                    rule-result)))
                              rules)]
                (loop [rets rets last-val nil]
                  (if-not (seq rets)
                    (make-rule last-val)
                    (let [ret (first rets)
                          val (first (filter map? ret))]
                      (when val
                        (swap! context update-in [:state] merge val))
                      (recur (rest rets) val))))))
            (finish-rule [value]
              (swap! context update-in [:state] merge value)
              (make-rule value))
            (make-rule [value]
              (if-let [rules (seq (find-rules))]
                (do
                  (swap! context update-in [:rules] (partial remove (set rules)))
                  (exec-rules rules))
                (-> value vals first)))]
      (make-rule init-state))))
