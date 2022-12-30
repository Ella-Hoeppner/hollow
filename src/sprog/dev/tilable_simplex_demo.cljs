(ns sprog.dev.tilable-simplex-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.chunks.noise :refer [tileable-simplex-2d-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (iglu->glsl
   tileable-simplex-2d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float
                mouse vec2}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue
                    (* (+ (snoiseTileable2D (vec2 0)
                                            (pow (vec2 25) mouse)
                                            (+ (* pos 3)
                                               (vec2 100 -20)))
                          1)
                       0.5))
            (= fragColor (vec4 noiseValue
                               noiseValue
                               noiseValue
                               1)))}))

(defn update-page! [gl _]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)
                                    "mouse" (mouse-pos)}})))

(defn init []
  (start-sprog! nil update-page!))
