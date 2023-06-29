(ns sprog.dev.simplex-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.chunks.noise :refer [simplex-2d-chunk
                                             simplex-3d-chunk]]
            [sprog.diglu.core :refer [iglu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def noise-2d-frag-source
  (iglu->glsl
   simplex-2d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue (* (+ (snoise2D (* pos 10)) 1) 0.5))
            (= fragColor (vec4 noiseValue
                               noiseValue
                               noiseValue
                               1)))}))

(def noise-3d-frag-source
  (iglu->glsl
   simplex-3d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue (* (+ (snoise3D (vec3 (* pos 10) time)) 
                                     1)
                                  0.5))
            (= fragColor (vec4 (vec3 noiseValue) 1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (let [[width height] (canvas-resolution)
          resolution [width height]
          half-width (* width 0.5)]
      (maximize-gl-canvas)
      (run-purefrag-shader! noise-2d-frag-source
                            [half-width height]
                            {"size" resolution})
      (run-purefrag-shader! noise-3d-frag-source
                            [half-width 0 half-width height]
                            {"size" resolution
                             "time" (u/seconds-since-startup)}))))

(defn init []
  (start-sprog! nil update-page!))
