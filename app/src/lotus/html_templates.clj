(ns lotus.html-templates
  (:use [io.pedestal.app.templates :only [tfn dtfn tnodes]]))

(defmacro lotus-templates
  []
  {:lotus-page (dtfn (tnodes "lotus.html" "hello") #{:id})})
