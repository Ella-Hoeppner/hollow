(ns sprog.dev.simplex-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
<<<<<<< HEAD
            [sprog.iglu.chunks.random :refer [simplex-2d-chunk
                                              simplex-3d-chunk]]
=======
            [sprog.iglu.chunks.noise :refer [simplex-2d-chunk
                                       simplex-3d-chunk]]
>>>>>>> ea5713937b187030ed6b4ea36bd2d41cf3d0da33
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce noise-2d-sprog-atom (atom nil))
(defonce noise-3d-sprog-atom (atom nil))

(def noise-2d-frag-source
  (iglu->glsl
   nil
   simplex-2d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (=float noiseValue (* (+ (snoise2D (* pos "10.0"))
                                           "1.0")
                                        "0.5"))
                  (= fragColor (vec4 noiseValue
                                     noiseValue
                                     noiseValue
                                     1)))}}))

(def noise-3d-frag-source
  (iglu->glsl
   nil
   simplex-3d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (=float noiseValue (* (+ (snoise3D (vec3 (* pos "10.0") time))
                                           "1.0")
                                        "0.5"))
                  (= fragColor (vec4 (vec3 noiseValue) 1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [width height]
        half-width (* width 0.5)
        split-resolution [half-width height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @noise-2d-sprog-atom
                        split-resolution
                        {:floats {"size" resolution}})
    (run-purefrag-sprog @noise-3d-sprog-atom
                        split-resolution
                        {:floats {"size" resolution
                                  "time" (u/seconds-since-startup)}}
                        {:offset [half-width 0]})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! noise-2d-sprog-atom (create-purefrag-sprog
                                 gl
                                 noise-2d-frag-source))
    (reset! noise-3d-sprog-atom (create-purefrag-sprog
                                 gl
                                 noise-3d-frag-source)))
  (update-page!))
