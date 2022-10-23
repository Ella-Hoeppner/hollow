(ns sprog.dev.tilable-simplex-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.iglu.chunks.noise :refer [tileable-simplex-2d-chunk]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.input.mouse :refer [mouse-pos]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(def frag-source
  (iglu->glsl
   nil
   tileable-simplex-2d-chunk
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
                  (=float noiseValue
                          (* (+ (snoiseTileable2D (vec2 0)
                                                  (pow (vec2 "25.0") mouse)
                                                  (+ (* pos "3.0")
                                                     (vec2 100 -20)))
                                "1.0")
                             "0.5"))
                  (= fragColor (vec4 noiseValue
                                     noiseValue
                                     noiseValue
                                     1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [width height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog gl
                        @sprog-atom
                        resolution
                        {:floats {"size" resolution
                                  "mouse" (mouse-pos)}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-purefrag-sprog gl frag-source)))
  (update-page!))
