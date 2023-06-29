(ns sprog.demos.pcg-hash
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.kudzu.chunks.noise :refer [pcg-hash-chunk]]
            [sprog.kudzu.chunks.misc :refer [pos-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (kudzu->glsl
   pos-chunk
   pcg-hash-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                now float}
     :outputs {fragColor vec4}
     :main ((= fragColor
               (vec4 (vec3 (rand-pcg (floor
                                      (* 19 (/ gl_FragCoord.xy
                                               size)
                                         now)))
                           (rand-pcg (floor
                                      (* 23 (/ gl_FragCoord.xy
                                               size)
                                         now)))
                           (rand-pcg (floor
                                      (* 43 (/ gl_FragCoord.xy
                                               size)
                                         now))))
                     1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {"size" (canvas-resolution)
      "now" (u/seconds-since-startup)})))

(defn init []
  (js/window.addEventListener "load" #(start-sprog! nil update-page!)))