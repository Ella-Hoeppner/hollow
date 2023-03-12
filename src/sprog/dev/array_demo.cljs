(ns sprog.dev.array-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-glsl
  (u/unquotable
   (iglu->glsl
    '{:version "300 es"
      :precision {float highp}
      :uniforms {size vec2
                 testUniform [float 10]}
      :outputs {fragColor vec4}
      :structs {TestStruct [x [float 5]
                            y int]}
      :functions
      {f {([[float 5] [float 5]] [float 5])
          ([x y]
           [float 5 [(+ [x 0] [y 0]) 0 0 0 0]])}}
      :main
      ((=vec2 pos (/ gl_FragCoord.xy size))
       (= [float 5]
          a
          [float 5 [1 2 3 4 5]])
       (= TestStruct
          t
          (TestStruct [float 5 [0 1 2 3 4]]
                      "5"))

       (= fragColor
          (vec4 pos
                0
                1)))})))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)})))

(defn init []
  (start-sprog! nil update-page!))
