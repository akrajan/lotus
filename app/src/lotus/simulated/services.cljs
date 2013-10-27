(ns lotus.simulated.services
  (:require [io.pedestal.app.protocols :as p]
            [io.pedestal.app.messages :as msg]
            [io.pedestal.app.util.platform :as platform]))

;; (def counters (atom {"abc" 0 "xyz" 0}))

;; (defn increment-counter [key t input-queue]
;;   (p/put-message input-queue {msg/type :swap
;;                               msg/topic [:other-counters key]
;;                               :value (get (swap! counters update-in [key] inc) key)})
;;   (platform/create-timeout t #(increment-counter key t input-queue)))

;; (defn receive-messages [input-queue]
;;   (increment-counter "abc" 2000 input-queue)
;;   (increment-counter "xyz" 5000 input-queue))

(defrecord SearchService [app]
  p/Activity
  (start [this])
  (stop [this]))

(def search-map {"install" [{:result "install electric fan"}
                            {:result "install AC"}
                            {:result "install TV"}]
                 "repair" [{:result "repair motor"}
                           {:result "repair fan"}
                           {:result "repair fridge"}]
                 "maintain" [{:result "mower lawn"}
                             {:result "walk pets"}]})

(defn prefix-match [search-str pairs]
  (some
   (fn [[k v]]
     (let [term-prefix (apply str (take (count search-str) k))]
       (if (= search-str term-prefix)
         v
         false)))
    pairs))

(defn dummy-search-fn [key]
  (let [key (.toLowerCase key)]
    (or (prefix-match key search-map) [])))

(defn services-fn [message input-queue]
  (let [search-response (dummy-search-fn (:value message))]
    (.log js/console (str "Sending message to server: " search-response))
    (p/put-message input-queue
                   {msg/type :search-result
                    msg/topic [:search :response]
                    :response search-response})))
