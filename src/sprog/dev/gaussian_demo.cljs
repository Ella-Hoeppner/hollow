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
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [html-image-texture]]))

(defonce gl-atom (atom nil))
(defonce html-image-atom (atom nil))

(def top-frag-source
  (iglu->glsl
   {}
   (create-gaussian-sample-chunk
    'tex
    (square-neighborhood 4 3))
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
               (gaussianSample pos
                               (/ 1 texSize)
                               (* 10 (clamp blurFactor 0.001 1)))))}))

(def bottom-frag-source
  (iglu->glsl
   {:gaussian-expression
    (partial prescaled-gaussian-sample-expression
             (square-neighborhood 4 3))}
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
                               (/ (vec2 :x :y) texSize))))
                10)))}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        half-height (* height 0.5)]
    (maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          top-frag-source
                          [width half-height]
                          {:floats {"size" [width height]
                                    "blurFactor" (first (mouse-pos))}
                           :textures {"tex" @html-image-atom}}
                          {:offset [0 half-height]})
    (run-purefrag-shader! gl
                          bottom-frag-source
                          [width half-height]
                          {:floats {"size" [width height]
                                    "blurFactor" (- 1 (first (mouse-pos)))}
                           :textures {"tex" @html-image-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (reset! html-image-atom (html-image-texture gl "img"))
    (update-page!)))
