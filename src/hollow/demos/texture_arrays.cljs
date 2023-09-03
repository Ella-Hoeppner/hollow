(ns hollow.demos.texture-arrays
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.textures :refer [create-tex]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.tools :refer [unquotable]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-glsl
  (unquotable
   (kudzu->glsl
    '{:precision {float highp}
      :uniforms {texs [sampler2D "2"]}
      :outputs {fragColor vec4}
      :main
      ((= fragColor
          (vec4 (mix (.rgb (texture [texs "0"] (vec2 0.5)))
                     (.rgb (texture [texs "1"] (vec2 0.5)))
                     0.5)
                1)))})))

(defn init-page! [gl]
  (with-context gl
    {:texs [(create-tex :f8 
                        1
                        {:data (js/Uint8Array. [255 0 0 0])})
            (create-tex :f8 
                        1
                        {:data (js/Uint8Array. [0 255 0 0])})]}))

(defn update-page! [{:keys [gl texs] :as state}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {:texs texs})
    state))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! init-page! update-page!)))
