(ns hollow.demos.pcg-hash
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.chunks.noise :refer [pcg-hash-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   pcg-hash-chunk
   '{:precision {float highp}
     :uniforms {resolution vec2
                time float}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (pixel-pos))
            (= fragColor
               (vec4 (vec3 (rand-pcg (floor
                                      (* 19 pos time)))
                           (rand-pcg (floor
                                      (* 23 pos time)))
                           (rand-pcg (floor
                                      (* 43 pos time))))
                     1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {:resolution (canvas-resolution)
      :time (u/seconds-since-startup)})
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))
