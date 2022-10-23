(ns sprog.dev.macro-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(def frag-source
  (iglu->glsl
   {:rand
    (fn
      ([] (rand))
      ([minimum maximum] (+ minimum (rand (- maximum minimum)))))}
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float
                mouse vec2}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (=float dist (distance pos 
                                         (vec2 [:rand]
                                               [:rand])))
                  (= fragColor
                     (if (> dist [:rand 0.1 0.5])
                       (vec4 1)
                       (vec4 0 0 0 1))))}}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [width height]]
    (square-maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog gl
                        @sprog-atom
                        resolution
                        {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-purefrag-sprog
                        gl
                        frag-source)))
  (update-page!))
