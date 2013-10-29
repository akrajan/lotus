(ns lotus.utils.autocompleter.utils.macros)

(defmacro dochan [[binding chan] & body]
  `(let [chan# ~chan]
     (cljs.core.async.macros/go
       (loop []
         (if-let [~binding (cljs.core.async/<! chan#)]
           (do
             ~@body
             (recur))
           :done)))))

(defmacro <? [expr]
  `(lotus.utils.autocompleter.utils.helpers/throw-err (cljs.core.async/<! ~expr)))
