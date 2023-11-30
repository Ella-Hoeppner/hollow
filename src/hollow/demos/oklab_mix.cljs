(ns hollow.demos.oklab-mix
  (:require [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.chunks.color.oklab :refer [mix-oklab-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   mix-oklab-chunk
   '{:precision {float highp}
     :uniforms {size vec2}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= frag-color
               (if (> pos.y (/ 2 3))
                 (vec4 (mix (vec3 0 0 1) (vec3 1) pos.x)
                       1)
                 (if (> pos.y (/ 1 3))
                   (vec4 (mix-oklab (vec3 0 0 1) (vec3 1) pos.x)
                         1)
                   (vec4 (mix-oklab (vec3 0 0 1) (vec3 1) pos.x 0.2)
                         1)))))}))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)})
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! init-page! nil)))
