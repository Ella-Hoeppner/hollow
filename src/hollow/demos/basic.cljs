(ns hollow.demos.basic
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core :refer-macros [with-context]
             :refer [start-hollow!]]))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader!
     (kudzu->glsl
      '{:precision {float highp}
        :uniforms {size vec2}
        :outputs {frag-color vec4}
        :main ((=vec2 pos (/ gl_FragCoord.xy size))
               (= frag-color (vec4 pos 0 1)))})
     (canvas-resolution)
     {"size" (canvas-resolution)})
    {}))

(defn init []
  (start-hollow! init-page! nil))
