(ns hollow.demos.sub-data
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.textures :refer [create-tex
                                           set-tex-sub-data!]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   '{:precision {float highp
                 sampler2D highp}
     :uniforms {size vec2
                tex sampler2D}
     :outputs {frag-color vec4}
     :main ((= frag-color (texture tex (/ gl_FragCoord.xy size))))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "tex" texture}))
  state)

(defn init-page! [gl]
  {:gl gl
   :texture (with-context gl
              (let [tex (create-tex :f8
                                    4
                                    {:filter-mode :nearest
                                     :data (js/Uint8Array.
                                            (take 64 (cycle
                                                      (list 0 0 0 255))))})]
                (set-tex-sub-data! tex
                                   [1 1]
                                   [2 2]
                                   (js/Uint8Array.
                                    (take 16 (cycle (list 255 0 0 255)))))
                tex))})

(defn init []
  (js/window.addEventListener "load" #(start-hollow! init-page! update-page!)))
