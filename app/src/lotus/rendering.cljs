(ns lotus.rendering
  (:require [domina :as dom]
            [domina.css :as dc]
            [domina.events :as de]
            [io.pedestal.app.messages :as msgs]
            [io.pedestal.app.protocols :as p]
            [io.pedestal.app.render.push :as render]
            [io.pedestal.app.render.push.templates :as templates]
            [io.pedestal.app.render.push.handlers.automatic :as d])
  (:require-macros [lotus.html-templates :as html-templates]))

(def templates (html-templates/lotus-templates))

(defn render-page [renderer [_ path] transmitter]
  (let [parent (render/get-parent-id renderer path)
        id (render/new-id! renderer path)
        html (templates/add-template renderer path (:lotus-page templates))]
    (dom/append! (dom/by-id parent) (html {:id id :message ""}))))

(defn render-message [renderer [_ path _ new-value] transmitter]
  (templates/update-t renderer path {:message new-value}))

(defn enable-search [r [_ _ _ messages] input-queue]
  (let [todo-input (dom/by-id "search-box")]
    (de/listen! todo-input :keyup
                (fn [e]
                  (when (= (.-keyCode (.-evt e)) 13)
                    (let [details (dom/value todo-input)
                          new-msgs (msgs/fill :search-with messages {:search-text details})]
                      (dom/set-value! todo-input "")
                      (doseq [m new-msgs]
                        (p/put-message input-queue m))))))))

(defn render-config []
  [[:transform-enable [:setup-search] enable-search]])
