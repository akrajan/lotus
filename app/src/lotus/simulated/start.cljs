(ns lotus.simulated.start
  (:require [io.pedestal.app.render.push.handlers.automatic :as d]
            [lotus.start :as start]
            [lotus.rendering :as rendering]
            [io.pedestal.app.protocols :as p]
            [lotus.simulated.services :as services]
            [io.pedestal.app :as app]
            [goog.Uri]
            ;; This needs to be included somewhere in order for the
            ;; tools to work.
            [io.pedestal.app-tools.tooling :as tooling]))

(defn param [name]
  (let [uri (goog.Uri. (.toString  (.-location js/document)))]
    (.getParameterValue uri name)))

(defn ^:export main []
  (let [app (start/create-app (rendering/render-config))
        services (services/->SearchService (:app app))]
    (app/consume-effects (:app app) services/services-fn)
    (p/start services)
    app))
