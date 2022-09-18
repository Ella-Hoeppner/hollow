(ns sprog.dev.simplex-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.iglu.chunks :refer [merge-chunks
                                       simplex-2d-chunk
                                       simplex-3d-chunk]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def start-time (u/now))
(defn get-time [] (/ (- (u/now) start-time) 1000))

(defonce gl-atom (atom nil))
(defonce noise-2d-sprog-atom (atom nil))
(defonce noise-3d-sprog-atom (atom nil))

(def noise-2d-frag-source
  (merge-chunks
   simplex-2d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (=float noiseValue (snoise (* pos "10.0")))
                  (= fragColor (vec4 noiseValue
                                     noiseValue
                                     noiseValue
                                     1)))}}))

(def noise-3d-frag-source
  (merge-chunks
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
                  (=float noiseValue (snoise (vec3 (* pos "10.0") time)))
                  (= fragColor (vec4 noiseValue
                                     noiseValue
                                     noiseValue
                                     1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [(* 0.5 width) height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @noise-2d-sprog-atom
                        resolution
                        {:floats {"size" resolution}})
    (run-purefrag-sprog @noise-3d-sprog-atom
                        resolution
                        {:floats {"size" resolution
                                  "time" (get-time)}}
                        {:offset [(* width 0.5) 0]})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! noise-2d-sprog-atom (create-purefrag-sprog
                                 gl
                                 (iglu->glsl noise-2d-frag-source)))
    (reset! noise-3d-sprog-atom (create-purefrag-sprog
                                 gl
                                 (iglu->glsl noise-3d-frag-source))))
  (update-page!))
