(ns sprog.dev.oklab-mix-demo
  (:require [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.kudzu.chunks.color :refer [mix-oklab-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (kudzu->glsl
   mix-oklab-chunk
   '{:version "300 es"
     :precision {float highp}
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
  (start-sprog! nil update-page!))
