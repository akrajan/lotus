(ns lotus.utils.autocompleter.core
  (:require [clojure.core.async :as async]))
(defn start-producers [c]
  (async/thread (loop [start 1]
                  (when (< start 500)
                    (async/<!! (async/timeout 250))
                    (async/>!! c 1)
                    (recur (inc start)))))

  (async/thread (loop [start 1]
                  (when (< start 100)
                    (async/<!! (async/timeout 1000))
                    (async/>!! c 2)
                    (recur (inc start)))))

  (async/thread (loop [start 1]
                  (when (< start 50)
                    (async/<!! (async/timeout 2000))
                    (async/>!! c 3)
                    (recur (inc start))))))

(defn start-consumer [c]
  (future
    
    (loop []
      (if-let [resp (async/<!! c)]
        (do (println resp)
            (recur))))))



