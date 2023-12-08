(ns hollow.demos.texture-depth
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.shaders :refer [run-shaders!
                                          run-purefrag-shader!]]
            [hollow.webgl.textures :refer [create-tex]]
            [hollow.webgl.core :refer-macros [with-context]
             :refer [start-hollow!
                     enable!]]))

(def vert-glsl
  (kudzu->glsl
   '{:precision {float highp}
     :uniforms {resolution vec2}
     :outputs {color vec3}
     :global ((= [vec3 "6"]
                 vertex-positions
                 [vec3
                  (vec3 0 0 0)
                  (vec3 0.75 0 0)
                  (vec3 0 0.75 0)
                  (vec3 0.25 0.25 0.5)
                  (vec3 1 0.25 0.5)
                  (vec3 0.25 1 0.5)]))
     :main ((= color (if (< gl_VertexID "3")
                       (vec3 1 0 0)
                       (vec3 0 1 0)))
            (= gl_Position
               (vec4 [vertex-positions gl_VertexID] 1)))}))

(def frag-glsl
  (kudzu->glsl
   '{:precision {float highp}
     :uniforms {resolution vec2}
     :outputs {frag-color vec4}
     :inputs {color vec3}
     :main ((= frag-color (vec4 color 1)))}))

(def copy-frag-glsl
  (kudzu->glsl
   '{:precision {float highp}
     :uniforms {tex sampler2D}
     :outputs {frag-color vec4}
     :main ((= frag-color (texelFetch tex (ivec2 gl_FragCoord.xy) "0")))}))

(defn init-page! [gl]
  (with-context gl
    (enable! :depth-test)
    (maximize-gl-canvas {:aspect-ratio 1})
    (let [resolution (canvas-resolution)]
      #_(let []
          (run-shaders! [vert-glsl frag-glsl]
                        resolution
                        {:resolution resolution}
                        {}
                        0
                        6))
      (let [tex (create-tex :f8
                            resolution
                            {:depth-buffer? true})]
        (run-shaders! [vert-glsl frag-glsl]
                      resolution
                      {:resolution resolution}
                      {}
                      0
                      6
                      {:target tex})
        (run-purefrag-shader! copy-frag-glsl
                              resolution
                              {:tex tex})))
    {}))

(defn init []
  (js/window.addEventListener "load"
                              #(start-hollow! init-page! nil)))
