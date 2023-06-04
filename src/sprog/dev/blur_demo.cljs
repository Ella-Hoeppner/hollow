(ns sprog.dev.blur-demo
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.postprocessing 
             :refer
             [create-gaussian-sample-chunk
              prescaled-gaussian-sample-expression
              square-neighborhood]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.webgl.core
             :refer [start-sprog!]
             :refer-macros [with-context]]))

(def top-frag-source
  (iglu->glsl
   (create-gaussian-sample-chunk :f8 (square-neighborhood 4 3))
   '{:version "300 es"
     :precision {float highp
                 sampler2D highp}
     :outputs {fragColor vec4}
     :uniforms {size vec2
                tex sampler2D
                blurFactor float}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec2 texSize (vec2 (textureSize tex "0")))
            (=float sampleDistFactor blurFactor)
            (= fragColor
               (gaussianSample tex
                               pos
                               (/ 1 texSize)
                               (* 10 (clamp blurFactor 0.001 1)))))}))

(def bottom-frag-source
  (iglu->glsl
   {:macros {:gaussian-expression
             #(prescaled-gaussian-sample-expression
               %
               (square-neighborhood 4 3)
               10)}}
   '{:version "300 es"
     :precision {float highp
                 sampler2D highp}
     :outputs {fragColor vec4}
     :uniforms {size vec2
                tex sampler2D
                blurFactor float}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec2 texSize (vec2 (textureSize tex "0")))
            (= fragColor
               (:gaussian-expression
                (texture tex
                         (+ pos
                            (* (clamp blurFactor 0 1)
                               (/ (vec2 :x :y) texSize)))))))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (let [[width height] (canvas-resolution)
          half-height (* height 0.5)]
      (maximize-gl-canvas)
      (run-purefrag-shader! top-frag-source
                            [0 half-height width half-height]
                             {"size" [width height]
                              "blurFactor" (first (mouse-pos))
                              "tex" texture})
      (run-purefrag-shader! bottom-frag-source
                            [width half-height]
                            {"size" [width height]
                             "blurFactor" (- 1 (first (mouse-pos)))
                             "tex" texture})))
  state)

(defn init-page! [gl]
  {:texture (with-context gl
              (html-image-tex "img"))})

(defn init []
  (start-sprog! init-page! update-page!))