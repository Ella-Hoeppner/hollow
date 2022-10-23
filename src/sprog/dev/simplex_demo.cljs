(ns sprog.dev.simplex-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.chunks.noise :refer [simplex-2d-chunk
                                             simplex-3d-chunk]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))

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
    (run-purefrag-shader! gl
                          noise-2d-frag-source
                          split-resolution
                          {:floats {"size" resolution}})
    (run-purefrag-shader! gl
                          noise-3d-frag-source
                          split-resolution
                          {:floats {"size" resolution
                                    "time" (u/seconds-since-startup)}}
                          {:offset [half-width 0]})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas))
  (update-page!))
