(ns sprog.webgl.core
  (:require [clojure.walk :refer [postwalk]]))

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
          'do)))
