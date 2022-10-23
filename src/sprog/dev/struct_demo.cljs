(ns sprog.dev.struct-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

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
    :signatures {main ([] void)
                 vec5dot ([vec5 vec5] float)}
    :functions {vec5dot
                ([v1 v2]
                 (+ (* v1.a v2.a)
                    (* v1.b v2.b)
                    (* v1.c v2.c)
                    (* v1.d v2.d)
                    (* v1.e v2.e)))
                main
                ([]
                 (=vec2 pos (/ gl_FragCoord.xy size))
                 (=vec5 v1 (vec5 "0.0" "1.0" "2.0" "3.0" "4.0"))
                 (=vec5 v2 (vec5 "0.0" "1.0" "2.0" "3.0" "4.0"))
                 (=float dotProduct (vec5dot v1 v2))
                 (= fragColor (vec4 (/ dotProduct "60.0") 0 0 1)))}})

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog gl
                        @sprog-atom
                        resolution
                        {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-purefrag-sprog gl frag-source)))
  (update-page!))
