(ns hollow.demos.fbm
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.chunks.noise :refer [simplex-2d-chunk
                                        fbm-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   simplex-2d-chunk
   fbm-chunk
   '{:precision {float highp}
     :uniforms {resolution vec2}
     :outputs {frag-color vec4}
     :main ((= frag-color
               (vec4 (vec3 (-> (fbm snoise2D
                                    2
                                    (pixel-pos)
                                    "10"
                                    0.75)
                               (+ 1)
                               (* 0.5)))
                     1)))}))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {:resolution (canvas-resolution)})
    {}))

(defn init []
  (start-hollow! init-page! nil))
