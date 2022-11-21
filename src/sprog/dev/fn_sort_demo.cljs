(ns sprog.dev.fn-sort-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [merge-chunks]]
            [clojure.walk :refer [postwalk-replace]]
            [sprog.webgl.core :refer [with-context]]))

(def fn-count 50)

(defonce gl-atom (atom nil))

(def frag-source
  (reduce merge-chunks
          (u/q
           {:version "300 es"
            :precision {float highp}
            :uniforms {size vec2}
            :outputs {fragColor vec4}
            :main ((= fragColor
                      (vec4 (~(symbol (str "f" fn-count)) 0) 0 0 1)))})
          (map (fn [i]
                 (postwalk-replace
                  {:fn-name (symbol (str "f" (inc i)))
                   :prev-fn-name (symbol (str "f" i))}
                  (if (zero? i)
                    '{:functions {:fn-name {([float] float)
                                            ([x] (+ x 0.01))}}}
                    '{:functions {:fn-name {([float] float)
                                            ([x] (+ (:prev-fn-name x)
                                                    0.01))}}})))
               (range fn-count))))

(defn update-page! []
  (with-context @gl-atom
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
