(ns sprog.dev.array-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
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
                 decisions [bool "3"]}
      :outputs {fragColor vec4}
      :functions
      {decide-colors {([[vec3 "2"] [bool "3"]] vec3)
                      ([c d]
                       (vec3
                        (.r (if [d "0"] [c "0"] [c "1"]))
                        (.g (if [d "1"] [c "0"] [c "1"]))
                        (.b (if [d "2"] [c "0"] [c "1"]))))}}
      :main
      ((= [vec3 "2"]
          color-vectors
          [vec3 "2" [(vec3 1 1 1)
                     (vec3 0 0 0)]])
       (= fragColor
          (vec4 (decide-colors color-vectors decisions)
                1)))})))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)
      "decisions" (mapv #(if (> (Math/sin (* u/TAU
                                             (+ (u/seconds-since-startup) %)))
                                0)
                           true
                           false)
                        (u/prange 3 true))})))

(defn init []
  (start-sprog! nil update-page!))
