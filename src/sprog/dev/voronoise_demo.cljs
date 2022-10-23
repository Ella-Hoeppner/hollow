(ns sprog.dev.voronoise-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-autosprog]]
            [sprog.iglu.chunks.noise :refer [voronoise-chunk]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))

(def noise-2d-frag-source
  (iglu->glsl
   nil
   voronoise-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                mouse vec2
                time float}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (=float noiseValue
                          (voronoise mouse.x
                                     mouse.y (+ (* pos "23.")
                                                (vec2 (cos time)
                                                      (sin time)))))
                  (= fragColor (vec4 (vec3 noiseValue) 1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width
                    gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-autosprog gl
                            noise-2d-frag-source
                            resolution
                            {:floats {"size" resolution
                                      "mouse" (mouse-pos)
                                      "time" (u/seconds-since-startup)}}))
  (js/requestAnimationFrame update-page!))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl))
  (update-page!))