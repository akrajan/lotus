(ns lotus.rendering
  (:require [domina :as dom]
            [domina.css :as dc]
            [domina.events :as de]
            [lotus.utils.autocompleter.autocomplete.core :as autocomplete]
            [cljs.core.async :refer [>! <! alts! chan sliding-buffer put! to-chan onto-chan close!]]
            [io.pedestal.app.messages :as msgs]
            [io.pedestal.app.protocols :as p]
            [io.pedestal.app.render.push :as render]
            [io.pedestal.app.render.push.templates :as templates]
            [io.pedestal.app.render.push.handlers.automatic :as d])
  (:require-macros [lotus.html-templates :as html-templates]
                   [cljs.core.async.macros :refer [go]]))

(def templates (html-templates/lotus-templates))

(defn render-page [renderer [_ path] transmitter]
  (let [parent (render/get-parent-id renderer path)
        id (render/new-id! renderer path)
        html (templates/add-template renderer path (:lotus-page templates))]
    (dom/append! (dom/by-id parent) (html {:id id :message ""}))))

(defn render-message [renderer [_ path _ new-value] transmitter]
  (templates/update-t renderer path {:message new-value}))

;; (defn enable-search [r [_ _ _ messages] input-queue]
;;   (let [todo-input (dom/by-id "autocomplete")]
;;     (de/listen! todo-input :keyup
;;                 (fn [e]
;;                   (let [details (dom/value todo-input)
;;                         new-msgs (msgs/fill :search-with messages {:search-text details})]
;;                     (if (empty? details)
;;                       (dom/destroy! (dc/sel "#autocomplete-menu li"))
;;                       (doseq [m new-msgs]
;;                         (p/put-message input-queue m))))
;;                   (when (= (.-keyCode (.-evt e)) 13)
;;                     (dom/set-value! todo-input ""))))))

(def completions-ref (atom (chan)))

(defn get-completions [messages input-queue]
  (fn [query]
    (doseq [m (msgs/fill :search-with messages {"search-text" query})]
      (p/put-message input-queue m))
    @completions-ref))

(defn enable-search [r [_ _ _ messages] input-queue]
  (.log js/console "inside enable-search")
  (let [ac (autocomplete/html-autocompleter
            (dom/by-id "autocomplete")
            (dom/by-id "autocomplete-menu")
            #(go [["Arun" "AKR"] ["Arun" "AKR"] ["Arun" "AKR"] ["Arun" "AKR"] ["Arun" "AKR"]])
            750)]
    (.log js/console "before go block")
    (go (while true
          (<! ac)))))

(defn update-search-result [r [_ _ _ messages] input-queue]
  (onto-chan @completions-ref [(mapv (juxt :result :result) messages)])
  (reset! completions-ref (chan)))


(defn dummy-fn [& _])

(defn render-config []
  [[:transform-enable [:setup-search] enable-search]
   [:value [:search :response] update-search-result]])
