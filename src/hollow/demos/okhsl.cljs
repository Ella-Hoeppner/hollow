(ns hollow.demos.okhsl
  (:require [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.chunks.color.oklab :refer [okhsl-chunk]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]
            [hollow.util :as u]))

(def frag-source
  (kudzu->glsl
   okhsl-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                hue float}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= frag-color (vec4 (okhsl->srgb (vec3 hue pos)) 1)))}))

(defn update-page! [{:keys [gl] :as state}]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {:size (canvas-resolution)
                           :hue (* (u/seconds-since-startup) 0.25)})
    state))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))
