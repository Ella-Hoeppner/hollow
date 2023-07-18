(ns hollow.demos.pcg-hash
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.chunks.noise :refer [pcg-hash-chunk]]
            [kudzu.chunks.misc :refer [pos-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   pos-chunk
   pcg-hash-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                now float}
     :outputs {fragColor vec4}
     :main ((= fragColor
               (vec4 (vec3 (rand-pcg (floor
                                      (* 19 (/ gl_FragCoord.xy
                                               size)
                                         now)))
                           (rand-pcg (floor
                                      (* 23 (/ gl_FragCoord.xy
                                               size)
                                         now)))
                           (rand-pcg (floor
                                      (* 43 (/ gl_FragCoord.xy
                                               size)
                                         now))))
                     1)))}))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {"size" (canvas-resolution)
      "now" (u/seconds-since-startup)})
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! init-page! nil)))
