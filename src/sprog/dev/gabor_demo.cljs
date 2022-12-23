(ns sprog.dev.gabor-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.noise :refer [gabor-noise-2d-chunk]]
            [sprog.iglu.chunks.misc :refer [pos-chunk]]
            [sprog.webgl.core :refer [with-context]]))

(defonce gl-atom (atom nil))

(def frag-glsl
  (iglu->glsl 
   pos-chunk 
   gabor-noise-2d-chunk
   (u/unquotable
    '{:version "300 es"
      :precision {float highp}
      :uniforms {size vec2
                 time float}
      :outputs {fragColor vec4}
      :main ((=float noiseValue
                     (gaborNoise ~(u/gen 50 (* 2 (Math/pow 40 (rand))))
                                 (-> (getPos)
                                     (* 2)
                                     (- 1))
                                 0.5))
             (= fragColor (vec4 (vec3 (* 0.5 (+ 1 noiseValue))) 1)))})))

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
