(ns sprog.dev.gaussian-demo
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.postprocessing 
             :refer
             [create-gaussian-sample-chunk
              prescaled-gaussian-sample-expression
              square-neighborhood]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.webgl.core :refer-macros [with-context]]))

(defonce gl-atom (atom nil))
(defonce html-image-atom (atom nil))

(def top-frag-source
  (iglu->glsl
   {}
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
            (=vec2 texSize (vec2 (textureSize tex i0)))
            (=float sampleDistFactor blurFactor)
            (= fragColor
               (gaussianSample tex
                               pos
                               (/ 1 texSize)
                               (* 10 (clamp blurFactor 0.001 1)))))}))

(def bottom-frag-source
  (iglu->glsl
   {:gaussian-expression
    #(prescaled-gaussian-sample-expression
      %
      (square-neighborhood 4 3)
      10)}
   '{:version "300 es"
     :precision {float highp
                 sampler2D highp}
     :outputs {fragColor vec4}
     :uniforms {size vec2
                tex sampler2D
                blurFactor float}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec2 texSize (vec2 (textureSize tex i0)))
            (= fragColor
               (:gaussian-expression
                (texture tex
                         (+ pos
                            (* (clamp blurFactor 0 1)
                               (/ (vec2 :x :y) texSize)))))))}))

(with-context @gl-atom
  (defn update-page! []
    (let [[width height] (canvas-resolution)
          half-height (* height 0.5)]
      (maximize-gl-canvas)
      (run-purefrag-shader! top-frag-source
                            [0 half-height width half-height]
                            {:floats {"size" [width height]
                                      "blurFactor" (first (mouse-pos))}
                             :textures {"tex" @html-image-atom}})
      (run-purefrag-shader! bottom-frag-source
                            [width half-height]
                            {:floats {"size" [width height]
                                      "blurFactor" (- 1 (first (mouse-pos)))}
                             :textures {"tex" @html-image-atom}})
      (js/requestAnimationFrame update-page!)))
  
  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! html-image-atom (html-image-tex "img"))
    (update-page!)))