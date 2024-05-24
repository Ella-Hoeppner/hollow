(ns hollow.demos.voronoise
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.chunks.noise :refer [voronoise-chunk]]
            [hollow.input.mouse :refer [mouse-pos]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def noise-2d-frag-source
  (kudzu->glsl
   voronoise-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                mouse vec2
                time float}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue
                    (voronoise (bi->uni mouse.x)
                               (bi->uni mouse.y)
                               (+ (* pos 23)
                                  (vec2 (cos time)
                                        (sin time)))))
            (= frag-color (vec4 (vec3 noiseValue) 1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! noise-2d-frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "mouse" (mouse-pos)
                           "time" (u/seconds-since-startup)})
    {}))

(defn init []
  (start-hollow! nil update-page!))
