(ns hollow.webgl.core
  (:require [clojure.walk :refer [postwalk]]))

(defmacro with-context [context & body]
  (let [contextful-functions '#{create-tex
                                delete-tex
                                run-purefrag-shader!
                                run-shaders!
                                maximize-gl-canvas
                                resize-gl-canvas
                                canvas-resolution
                                create-boj!
                                clear!
                                enable!
                                disable!
                                set-stencil-func!
                                set-stencil-op!
                                set-color-mask!
                                copy-html-image-data!
                                html-image-tex
                                max-tex-size
                                set-boj-data!
                                tex-data-array
                                set-tex-data!
                                set-tex-sub-data!}]
    (conj (postwalk (fn [form]
                      (if (and (seq? form)
                               (contextful-functions (first form)))
                        (list '-> context form)
                        form))
                    body)
          'do)))
