(ns hollow.demos.blur
  (:require [hollow.util :as u]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.chunks.postprocessing
             :refer
             [create-gaussian-sample-chunk
              prescaled-gaussian-sample-expression
              square-neighborhood]]
            [hollow.input.mouse :refer [mouse-pos]]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.textures :refer [html-image-tex]]
            [hollow.webgl.core
             :refer [start-hollow!]
             :refer-macros [with-context]]))

(def top-frag-source
  (kudzu->glsl
   (create-gaussian-sample-chunk :f8 (square-neighborhood 4 3))
   '{:precision {float highp
                 sampler2D highp}
     :outputs {frag-color vec4}
     :uniforms {size vec2
                tex sampler2D
                blurFactor float}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec2 texSize (vec2 (textureSize tex "0")))
            (=float sampleDistFactor blurFactor)
            (= frag-color
               (gaussianSample tex
                               pos
                               (/ 1 texSize)
                               (* 10 (clamp blurFactor 0.001 1)))))}))

(def bottom-frag-source
  (kudzu->glsl
   {:macros {:gaussian-expression
             #(prescaled-gaussian-sample-expression
               %
               (square-neighborhood 4 3)
               10)}}
   '{:precision {float highp
                 sampler2D highp}
     :outputs {frag-color vec4}
     :uniforms {size vec2
                tex sampler2D
                blurFactor float}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec2 texSize (vec2 (textureSize tex "0")))
            (= frag-color
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
                             "blurFactor" (* 0.5 (inc (first (mouse-pos))))
                             "tex" texture})
      (run-purefrag-shader! bottom-frag-source
                            [width half-height]
                            {"size" [width height]
                             "blurFactor" (- 1 
                                             (* 0.5 (inc (first (mouse-pos)))))
                             "tex" texture})))
  state)

(defn init-page! [gl]
  {:texture (with-context gl
              (html-image-tex "img"))})

(defn init []
  (start-hollow! init-page! update-page!))
