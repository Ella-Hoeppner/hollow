(ns sprog.demos.tilable-simplex
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.kudzu.chunks.noise :refer [tileable-simplex-2d-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (kudzu->glsl
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

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "mouse" (mouse-pos)})))

(defn init []
  (js/window.addEventListener "load" #(start-sprog! nil update-page!)))
