(ns sprog.dev.gabor-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.noise :refer [get-gabor-noise-2d-chunk]]
            [sprog.iglu.chunks.misc :refer [pos-chunk]]
            [sprog.webgl.core :refer [with-context]]))

(defonce gl-atom (atom nil))

(def frag-glsl
  (iglu->glsl 
   pos-chunk
   (get-gabor-noise-2d-chunk (u/gen 16 (* 2 (Math/pow 10 (rand))))
                             {:exclude-bandwidth? true})
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float}
     :outputs {fragColor vec4}
     :main ((=float noiseValue
                    (gaborNoise (- (* (getPos) 2) 1)))
            (= fragColor (vec4 (vec3 (* 0.5 (+ 1 noiseValue))) 1)))}))

(defn update-page! []
  (with-context @gl-atom
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)
               "time" (u/seconds-since-startup)}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
