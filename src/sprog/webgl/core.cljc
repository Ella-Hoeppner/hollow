(ns sprog.webgl.core
  #?(:cljs (:require-macros [sprog.webgl.core]))
  #?(:clj (:require [clojure.walk :refer [postwalk]])))

#?(:clj
   (defmacro with-context [context & body]
     (let [contextful-functions '#{create-tex
                                   run-purefrag-shader!
                                   run-shaders!
                                   maximize-gl-canvas
                                   square-maximize-gl-canvas
                                   canvas-resolution
                                   create-boj!
                                   copy-html-image-data!
                                   html-image-tex}]
       (conj (postwalk (fn [form]
                         (if (and (seq? form)
                                  (contextful-functions (first form)))
                           (list '-> context form)
                           form))
                       body)
             'do))))
