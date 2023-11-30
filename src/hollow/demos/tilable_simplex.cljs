(ns hollow.demos.tilable-simplex
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.chunks.noise :refer [tileable-simplex-2d-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.input.mouse :refer [mouse-pos]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   tileable-simplex-2d-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                time float
                mouse vec2}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue
                    (* (+ (snoiseTileable2D (vec2 0)
                                            (pow (vec2 25) (bi->uni mouse))
                                            (+ (* pos 3)
                                               (vec2 100 -20)))
                          1)
                       0.5))
            (= frag-color (vec4 noiseValue
                               noiseValue
                               noiseValue
                               1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "mouse" (mouse-pos)})
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))
