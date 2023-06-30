(ns sprog.demos.gabor
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.kudzu.chunks.noise :refer [gabor-noise-chunk]]
            [sprog.kudzu.chunks.misc :refer [pos-chunk]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-glsl
  (kudzu->glsl
   pos-chunk
   gabor-noise-chunk
   (u/unquotable
    '{:precision {float highp}
      :uniforms {size vec2
                 time float}
      :outputs {fragColor vec4}
      :main ((= fragColor
                (-> (gaborNoise 4
                                ~(u/gen 30 (Math/pow 200 (rand)))
                                (-> (getPos)
                                    (* 2)
                                    (- 1)
                                    (vec4 (sin time)
                                          (cos time)))
                                0.1)
                    (+ 1)
                    (* 0.5)
                    vec3
                    (vec4 1))))})))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)
      "time" (* 0.01 (u/seconds-since-startup))})))

(defn init []
  (js/window.addEventListener "load" #(start-sprog! nil update-page!)))
