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
                 colors [vec3 2]
                 decisions [int 3]}
      :outputs {fragColor vec4}
      :functions
      {decideColors {([[vec3 2] [int 3]] vec3)
                     ([c d]
                      (vec3
                       (.r (if (== [d 0] "1") [c 0] [c 1]))
                       (.g (if (== [d 1] "1") [c 0] [c 1]))
                       (.b (if (== [d 2] "1") [c 0] [c 1]))))}}
      :main
      ((= fragColor
          (vec4 (decideColors colors decisions)
                1)))})))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)
      "colors" [1 1 1
                0 0 0]
      "decisions" (mapv #(if (> (Math/sin (* u/TAU
                                             (+ (u/seconds-since-startup) %)))
                                0)
                           1
                           0)
                        (u/prange 3 true))})))

(defn init []
  (start-sprog! nil update-page!))
