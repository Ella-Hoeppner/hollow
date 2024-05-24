(ns hollow.demos.simplex
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.chunks.noise :refer [simplex-2d-chunk
                                        simplex-3d-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def noise-2d-frag-source
  (kudzu->glsl
   simplex-2d-chunk
   '{:precision {float highp}
     :uniforms {size vec2}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue (* (+ (snoise2D (* pos 10)) 1) 0.5))
            (= frag-color (vec4 noiseValue
                               noiseValue
                               noiseValue
                               1)))}))

(def noise-3d-frag-source
  (kudzu->glsl
   simplex-3d-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                time float}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float noiseValue (* (+ (snoise3D (vec3 (* pos 10) time))
                                     1)
                                  0.5))
            (= frag-color (vec4 (vec3 noiseValue) 1)))}))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (let [[width height] (canvas-resolution)
          resolution [width height]
          half-width (* width 0.5)]
      (run-purefrag-shader! noise-2d-frag-source
                            [half-width height]
                            {"size" resolution})
      (run-purefrag-shader! noise-3d-frag-source
                            [half-width 0 half-width height]
                            {"size" resolution
                             "time" (u/seconds-since-startup)}))
    {}))

(defn init []
  (start-hollow! nil update-page!))
