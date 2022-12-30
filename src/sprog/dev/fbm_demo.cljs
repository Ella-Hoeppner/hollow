(ns sprog.dev.fbm-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.iglu.chunks.noise :refer [simplex-2d-chunk
                                             fbm-chunk]]
            [sprog.iglu.chunks.misc :refer [pos-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core 
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (iglu->glsl
   pos-chunk
   simplex-2d-chunk
   fbm-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :main ((= fragColor
               (vec4 (vec3 (-> (fbm snoise2D
                                    2
                                    (* (getPos) 10)
                                    "5"
                                    0.9)
                               (+ 1)
                               (* 0.5)))
                     1)))}))

(defn update-page! [gl _]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)}}))
  gl)

(defn init []
  (start-sprog! nil update-page!))
