(ns sprog.dev.startup
  (:require [sprog.dev.basic-demo
             :rename
             {init init-basic-demo}]
            [sprog.dev.multi-texture-output-demo
             :rename
             {init init-multi-texture-output-demo}]
            [sprog.dev.fn-sort-demo
             :rename
             {init init-fn-sort-demo}]
            [sprog.dev.pixel-sort-demo
             :rename
             {init init-pixel-sort-demo}]))

(defn init []
  #_(init-basic-demo)
  #_(init-multi-texture-output-demo)
  #_(init-fn-sort-demo)
  (init-pixel-sort-demo))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
