(ns sprog.dev.basic-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer-macros [with-context]
             :refer [start-sprog!]]))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     '{:version "300 es"
       :precision {float highp}
       :uniforms {size vec2}
       :outputs {fragColor vec4}
       :main ((=vec2 pos (/ gl_FragCoord.xy size))
              (= fragColor (vec4 pos 0 1)))}
     (canvas-resolution)
     {"size" (canvas-resolution)})))

(defn init []
  (start-sprog! nil update-page!))
