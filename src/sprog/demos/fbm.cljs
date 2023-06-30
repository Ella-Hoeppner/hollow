(ns sprog.demos.fbm
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.kudzu.chunks.noise :refer [simplex-2d-chunk
                                              fbm-chunk]]
            [sprog.kudzu.chunks.misc :refer [pos-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (kudzu->glsl
   pos-chunk
   simplex-2d-chunk
   fbm-chunk
   '{:precision {float highp}
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

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {"size" (canvas-resolution)})))

(defn init []
  (js/window.addEventListener "load" #(start-sprog! nil update-page!)))
