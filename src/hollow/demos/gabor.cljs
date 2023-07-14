(ns hollow.demos.gabor
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.chunks.noise :refer [gabor-noise-chunk]]
            [kudzu.chunks.misc :refer [pos-chunk]]
            [kudzu.tools :refer [unquotable]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-glsl
  (kudzu->glsl
   pos-chunk
   gabor-noise-chunk
   (unquotable
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
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))
