(ns sprog.dev.startup
  (:require [sprog.dev.fn-sort-demo
             :rename
             {init init-basic-demo}]
            [sprog.dev.multi-texture-output-demo
             :rename
             {init init-multi-texture-output-demo}]
            [sprog.dev.fn-sort-demo
             :rename
             {init init-fn-sort-demo}]))

(defn init []
  #_(init-basic-demo)
  #_(init-multi-texture-output-demo)
  (init-fn-sort-demo))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
