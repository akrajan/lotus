(ns ^:shared lotus.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app.messages :as msg]))

(defn set-value-transform [old-value message]
  (:value message))


(defn init-search-box [_]
  (.log js/console "inside init-search-box")
  [[:transform-enable [:search] [{msg/type :search-with msg/topic [:search] (msg/param :search-text) ""}]]])


(def example-app
  {:version 2
   :transform [[:set-value [:greeting] set-value-transform]]
   ;; :emit {:init init-search-box
   ;;        [#{[:**]} (app/default-emitter [])]}
   })

