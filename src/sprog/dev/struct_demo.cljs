(ns sprog.dev.struct-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer-macros [with-context]]))

(defonce gl-atom (atom nil))

(def frag-source
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2}
    :outputs {fragColor vec4}
    :structs {vec5 [a float
                    b float
                    c float
                    d float
                    e float]}
    :functions {vec5dot
                {([vec5 vec5] float)
                 ([v1 v2]
                  (+ (* v1.a v2.a)
                     (* v1.b v2.b)
                     (* v1.c v2.c)
                     (* v1.d v2.d)
                     (* v1.e v2.e)))}}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (=vec5 v1 (vec5 0 1 2 3 4))
           (=vec5 v2 (vec5 0 1 2 3 4))
           (=float dotProduct (vec5dot v1 v2))
           (= fragColor (vec4 (/ dotProduct 60) 0 0 1)))})

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
