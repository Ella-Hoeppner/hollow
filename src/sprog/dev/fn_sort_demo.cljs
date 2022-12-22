(ns sprog.dev.fn-sort-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [combine-chunks]]
            [sprog.webgl.core :refer [with-context]]))

(def fn-count 50)

(defonce gl-atom (atom nil))

(def frag-source
  (u/unquotable
   (reduce combine-chunks
           '{:version "300 es"
             :precision {float highp}
             :uniforms {size vec2}
             :outputs {fragColor vec4}
             :main ((= fragColor
                       (vec4 (~(symbol (str "f" fn-count)) 0) 0 0 1)))}
           (map (fn [i]
                  '{:functions
                    {~(symbol (str "f" (inc i)))
                     {([float] float)
                      ([x] (+ ~(if (zero? i)
                                 'x
                                 '(~(symbol (str "f" i)) x))
                              0.01))}}})
                (range fn-count)))))

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
