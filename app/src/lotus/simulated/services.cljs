(ns lotus.simulated.services
  (:require [io.pedestal.app.protocols :as p]
            [io.pedestal.app.messages :as msg]
            [io.pedestal.app.util.platform :as platform]))

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
    (if (empty? key)
      []
      (or (prefix-match key search-map) []))))

(defn services-fn [message input-queue]
  (let [search-response (dummy-search-fn (:value message))]
    (p/put-message input-queue
                   {msg/type :search-result
                    msg/topic [:search :response]
                    :response search-response})))
