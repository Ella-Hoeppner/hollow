(ns sprog.dev.multi-texture-output-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.core :refer-macros [with-context]
             :refer [start-sprog!]]))

(def texture-resolution 8)

(def render-frag-source
  (kudzu->glsl
   {:constants {:texture-resolution texture-resolution}}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor0 vec4
               fragColor1 vec4}
     :layout {fragColor0 0
              fragColor1 1}
     :main ((=vec2 pos (/ gl_FragCoord.xy :texture-resolution))
            (= fragColor0 (vec4 pos
                                0
                                1))
            (= fragColor1 (vec4 0
                                pos
                                1)))}))

(def draw-frag-source
  (kudzu->glsl
   '{:precision {float highp
                 sampler2D highp}
     :uniforms {size vec2
                tex1 sampler2D
                tex2 sampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= fragColor
               (if (< pos.x 0.5)
                 (texture tex1 (* pos (vec2 2 1)))
                 (texture tex2 (* (- pos (vec2 0.5 0))
                                  (vec2 2 1))))))}))

(defn update-page! [{:keys [gl tex1 tex2] :as state}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! draw-frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "tex1" tex1
                           "tex2" tex2}))
  state)

(defn init-page! [gl]
  (with-context gl
    (let [[tex1 tex2]
          (u/gen 2 (create-tex :f8
                               texture-resolution
                               {:filter-mode :nearest}))]
      (run-purefrag-shader! render-frag-source
                            texture-resolution
                            {}
                            {:target [tex1 tex2]})
      {:tex1 tex1
       :tex2 tex2})))

(defn init []
  (start-sprog! init-page!
                update-page!))
