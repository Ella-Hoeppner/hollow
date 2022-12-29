(ns sprog.dev.params-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer [with-context
                                      start-update-loop!]]
            [sprog.iglu.core :refer [inline-float-uniforms]]))

(def shader-keywords
  {:r 1
   :g 1
   :b 0})

(def shader-source
  (inline-float-uniforms
   (keys shader-keywords)
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= fragColor (vec4 :r :g :b 1)))}))

(defn update-page! [gl]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! shader-source
                          (canvas-resolution)
                          {:floats (merge shader-keywords
                                          {"size" (canvas-resolution)})}))
  gl)

(defn init []
  (start-update-loop! update-page! (create-gl-canvas true)))
