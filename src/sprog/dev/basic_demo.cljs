(ns sprog.dev.basic-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.diglu.core :refer [iglu->glsl]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer-macros [with-context]
             :refer [start-sprog!]]))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     (iglu->glsl
      '{:version "300 es"
        :precision {float highp}
        :uniforms {size vec2}
        :outputs {fragColor vec4}
        :functions {pos->color
                    (vec4
                     [pos vec2]
                     (vec4 pos 0 1))}
        :main ((=vec2 pos (/ gl_FragCoord.xy size))
               (= fragColor (pos->color pos)))})
     (canvas-resolution)
     {"size" (canvas-resolution)})))

(defn init []
  (start-sprog! nil update-page!))
