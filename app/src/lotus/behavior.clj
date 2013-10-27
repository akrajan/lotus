(ns ^:shared lotus.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app.messages :as msg]
              [io.pedestal.app :as app]))

(defn set-value-transform [old-value message]
  (:value message))


(defn init-search-box [_]
  [[:transform-enable [:setup-search] :search-with
    [{msg/topic [:search :text] (msg/param :search-text) ""}]]])

(defn search-text-fn [old inputs]
  (:search-text inputs))

(defn search-with [search-key]
  [{msg/type :search-with msg/topic [:search :text] :value search-key}])

(def example-app
  {:version 2
   :transform [[:search-with [:search :text] search-text-fn]]
   :effect #{[#{[:search :text]} search-with :single-val]}
   :emit [{:in #{[:*]} :fn (app/default-emitter []) :init init-search-box}]})

