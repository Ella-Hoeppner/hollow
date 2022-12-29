(ns sprog.webgl.core
  #?(:cljs (:require-macros [sprog.webgl.core]))
  #?(:clj (:require [clojure.walk :refer [postwalk]])))

#?(:clj
   (defmacro with-context [context & body]
     (let [contextful-functions '#{create-tex
                                   delete-tex
                                   run-purefrag-shader!
                                   run-shaders!
                                   maximize-gl-canvas
                                   canvas-resolution
                                   create-boj!
                                   copy-html-image-data!
                                   html-image-tex
                                   max-texture-size}]
       (conj (postwalk (fn [form]
                         (if (and (seq? form)
                                  (contextful-functions (first form)))
                           (list '-> context form)
                           form))
                       body)
             'do))))

#?(:cljs
   (defn start-update-loop!
     ([update-fn]
      ((fn f []
         (update-fn)
         (js/requestAnimationFrame f))))
     ([update-fn initial-state]
      ((fn f [state]
         (js/requestAnimationFrame (partial f (update-fn state))))
       initial-state))))