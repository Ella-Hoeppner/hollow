(ns sprog.dev.startup
  (:require [sprog.dev.basic-demo
             :rename
             {init init-basic-demo}]
            [sprog.dev.multi-texture-output-demo
             :rename
             {init init-multi-texture-output-demo}]))

(defn init []
  #_(init-basic-demo)
  (init-multi-texture-output-demo))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
