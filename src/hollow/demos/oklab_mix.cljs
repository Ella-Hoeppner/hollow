(ns hollow.demos.oklab-mix
  (:require [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            (hollow.input.mouse :refer [mouse-pos])
            [kudzu.chunks.color :refer [mix-oklab-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   mix-oklab-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                mouse vec2
                tex sampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= fragColor
               (if (> pos.y (/ 2 3))
                 (vec4 (mix (vec3 0 0 1) (vec3 1) pos.x)
                       1)
                 (if (> pos.y (/ 1 3))
                   (vec4 (mixOklab (vec3 0 0 1) (vec3 1) pos.x)
                         1)
                   (vec4 (mixOklab (vec3 0 0 1) (vec3 1) pos.x 0.2)
                         1)))))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "mouse" (mouse-pos)})))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))