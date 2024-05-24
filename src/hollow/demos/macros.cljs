(ns hollow.demos.macros
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   {:macros {:rand (fn ([] (rand))
                     ([minimum maximum]
                      (+ minimum
                         (rand (- maximum minimum)))))}}
   '{:precision {float highp}
     :uniforms {size vec2
                time float
                mouse vec2}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float dist (distance pos
                                   (vec2 (:rand) (:rand))))
            (= frag-color
               (if (> dist (:rand))
                 (vec4 1)
                 (vec4 0 0 0 1))))}))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)})
    {}))

(defn init []
  (start-hollow! init-page! nil))
