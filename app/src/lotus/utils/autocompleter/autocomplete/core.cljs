(ns lotus.utils.autocompleter.autocomplete.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! <! alts! chan sliding-buffer put! timeout]]
            [lotus.utils.autocompleter.responsive.core :as resp]
            [lotus.utils.autocompleter.utils.dom :as dom]
            [lotus.utils.autocompleter.utils.helpers :as h]
            [lotus.utils.autocompleter.utils.reactive :as r]
            [goog.userAgent :as ua]
            [goog.events :as events]
            [goog.events.EventType]
            [clojure.string :as string]))

 
(def base-url "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defprotocol IHideable
  (-hide! [view])
  (-show! [view]))

(defprotocol ITextField
  (-set-text! [field txt])
  (-text [field]))

(defprotocol IUIList
  (-set-items! [list items]))

(defn menu-proc
  ([select cancel menu view-data]
  (.log js/console "inside menu-proc(a)")
     (menu-proc select cancel menu view-data view-data))
  ([select cancel menu view-data selection-data]
  (.log js/console "inside menu-proc(b)")
     (let [ctrl (chan)
           sel  (->> (resp/selector (resp/highlighter select menu ctrl)
                                    menu
                                    view-data
                                    selection-data)
                     (r/filter vector?)
                     (r/map second))]
       (go (let [[v sc] (alts! [cancel sel])]
             (do (>! ctrl :exit)
                 (if (or (= sc cancel)
                         (= v ::resp/none))
                   ::cancel
                   v)))))))


(defn autocompleter* [{:keys [focus query select cancel menu] :as opts}]
  (.log js/console "inside autocompleter*")
  (let [out (chan)
        [query raw] (r/split r/throttle-msg? query)]
    (go (loop [view-items nil
               data-items nil
               focused false]
          (let [[v sc] (alts! [raw cancel focus query select])]
            (.log js/console "focused = " focused "view-items = " view-items)
            (cond
             (= sc raw) (.log js/console "raw " " v =" (str v))
             (= sc channel) (.log js/console "cancel"" v =" (str v))
             (= sc focus) (.log js/console "focus"" v =" (str v))
             (= sc query) (.log js/console "query"" v =" (str v))
             (= sc select) (.log js/console "select"" v =" (str v)))
            
            (cond
             (= sc focus)
             (do
               (.log js/console "inside focus")
               (recur view-items data-items true))

             (= sc cancel)
             (do (.log js/console "inside cancel")
                 (-hide! menu)
                 (>! (:query-ctrl opts) (h/now))
                 (recur view-items data-items (not= v :blur)))

             (and focused (= sc query))
             (do (.log js/console "FOCUSED & QUERIED")
                 (let [t (timeout 1500)
                       [v c] (alts! [cancel ((:completions opts) (second v))])]
                   (.log js/console "inside focus")
                   (if (or (= c cancel) (= c t) (zero? (count v)))
                     (do (.log js/console "inside focus A")
                         (-hide! menu)
                         (recur nil nil (not= v :blur)))
                     (do (.log js/console "inside focus B")
                         (-show! menu)
                         (let [view-data (map first v)
                               select-data (map second v)]
                           (.log js/console "view-data = " (str view-data))
                           (.log js/console "select-data = " (str select-data))
                           (-set-items! menu view-data)
                           (recur view-data select-data focused))))))

             (and view-items (= sc select))
             (let [_ (reset! (:selection-state opts) true)
                   _ (>! (:query-ctrl opts) (h/now))
                   _ (.log js/console "view-items: " view-items)
                   choice (<! ((:menu-proc opts) (r/concat [v] select)
                               (r/fan-in [raw cancel])
                               menu
                               view-items
                               data-items))]
               (reset! (:selection-state opts) false)
               (-hide! menu)
               (if (= choice ::cancel)
                 (recur nil nil (not= v :blur))
                 (let [[view-select data-select] choice]
                   (-set-text! (:input opts) view-select)
                   (>! out data-select)
                   (recur nil nil focused))))

             :else
             (recur view-items data-items focused)))))
    out))


(defn less-than-ie9? []
  (.log js/console "inside less-than-ie9?")
  (and ua/IE (not (ua/isVersion 9))))

(extend-type js/HTMLInputElement
  ITextField
  (-set-text! [field text]
    (set! (.-value field) text))
  (-text [field]
    (.-value field)))

(extend-type js/HTMLUListElement
  IHideable
  (-hide! [list]
    (dom/add-class! list "hidden"))
  (-show! [list]
    (dom/remove-class! list "hidden"))

  IUIList
  (-set-items! [list items]
    (->> (for [item items] (str "<li>" item "</li>"))
      (apply str)
      (dom/set-html! list))))

(defn menu-item-event [menu input type]
  (.log js/console "inside menu-item-event")
  (->> (r/listen menu type
         (fn [e]
           (when (dom/in? e menu)
             (.preventDefault e))
           (when (less-than-ie9?)
             (.focus input)))
         (chan (sliding-buffer 1)))
    (r/map
      (fn [e]
        (let [li (dom/parent (.-target e) "li")]
          (h/index-of (dom/by-tag-name menu "li") li))))))

(defn html-menu-events [input menu allow-tab?]
  (.log js/console "inside html-menu-events")
  (r/fan-in
    [;; keyboard menu controls, tab special handling
     (->> (r/listen input :keydown
            (fn [e]
              (when (and @allow-tab?
                         (= (.-keyCode e) resp/TAB))
                (.preventDefault e))))
       (r/map resp/key-event->keycode)
       (r/filter
         (fn [kc]
           (and (resp/KEYS kc)
                (or (not= kc resp/TAB)
                    @allow-tab?))))
       (r/map resp/key->keyword))
     ;; hover events, index of hovered child
     (r/hover-child menu "li")
     ;; need to handle menu clicks
     (->> (r/cyclic-barrier
            [(menu-item-event menu input :mousedown)
             (menu-item-event menu input :mouseup)])
       (r/filter (fn [[d u]] (= d u)))
       (r/always :select))]))

(defn relevant-keys [kc]
  (.log js/console "inside relevant-keys")
  (or (= kc 8)
      (and (> kc 46)
           (not (#{91 92 93} kc)))))

(defn html-input-events [input]
  (.log js/console "inside html-input-events")
  (->> (r/listen input :keydown)
    (r/remove (fn [e] (.-platformModifierKey e)))
    (r/map resp/key-event->keycode)
    (r/filter relevant-keys)
    (r/map #(-text input))
    (r/split #(not (string/blank? %)))))

(defn ie-blur [input menu selection-state]
  (.log js/console "inside ie-blur")
  (let [out (chan)]
    (events/listen input goog.events.EventType.KEYDOWN
      (fn [e]
        (when (and (= (.-keyCode e) resp/TAB) (not @selection-state))
          (put! out (h/now)))))
    (events/listen js/document.body goog.events.EventType.MOUSEDOWN
      (fn [e]
        (when-not (some #(dom/in? e %) [menu input])
          (put! out (h/now)))))
    out))

(defn html-autocompleter [input menu completions throttle]
  (.log js/console "inside html-autocompleter")
  (let [selection-state (atom false)
        query-ctrl (chan)
        [filtered removed] (html-input-events input)]
    (when (less-than-ie9?)
      (events/listen menu goog.events.EventType.SELECTSTART
        (fn [e] false)))
    (-set-text! input "")
    (autocompleter*
     {:focus (r/always :focus (r/listen input :focus))
      :query (r/throttle* (r/distinct filtered) throttle (chan) query-ctrl)
      :query-ctrl query-ctrl
      :select (html-menu-events input menu selection-state)
      :cancel (r/fan-in
               [removed
                (r/always :blur
                          (if-not (less-than-ie9?)
                            (r/listen input :blur)
                            (ie-blur input menu selection-state)))])
      :input input
      :menu menu
      :menu-proc menu-proc
      :completions completions
      :selection-state selection-state})))

;; ;; =============================================================================
;; ;; Example

(defn wikipedia-search [query]
  (go
   (let [response (<! (r/jsonp (str base-url query)))]
     (let [completions (nth response 1)]
       (mapv vector
             completions
             completions)))))

;; ;; (defn ^:export main []
;; ;;   (let [ac (html-autocompleter
;; ;;             (dom/by-id "autocomplete")
;; ;;             (dom/by-id "autocomplete-menu")
;; ;;             wikipedia-search
;; ;;             750)]
;; ;;     (go (while true (.log js/console (<! ac))))))

