(ns iokov.handlers)

(defn callback-handler [input output f input-k cb]
  (f input cb)
  :async)

(defn promise-handler [input output f input-k cb]
  (cb @(f input))
  :async)

(defn time-handler [state]
  (let [start (System/currentTimeMillis)]
    (fn [input output f input-k cb]
      (when input
        (swap! state conj [input output (- (System/currentTimeMillis) start)]))
      nil)))

(defn log-handler
  ([] (log-handler #(println (str "--- [" (java.util.Date.) "]" %1 %2))))
  ([log] (fn [input output f input-k cb]
           (log input output)
           nil)))

(defn lift [input output f input-k cb]
  "iokov handlers that allows using simple functions or values"
  (let [all-vals (-> input-k (zipmap (repeat nil)) (merge input) vals)]
    {(first output) (if (fn? f)
                      (apply f all-vals)
                      f)}))
