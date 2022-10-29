(ns sprog.dev.pixel-sort-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-f8-tex
                                          html-image-texture]]
            [sprog.input.mouse :refer [mouse-pos]]))

(def sort-resolution 1000)

(defonce gl-atom (atom nil))

(defonce frame-atom (atom nil))

(defonce texs-atom (atom nil))

; adapted from https://www.shadertoy.com/view/wsSczw
(def logic-frag-source
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2
               tex sampler2D
               frame int
               threshold float}
    :outputs {fragColor vec4}
    :signatures {grayscale ([vec3] float)}
    :functions {grayscale ([c] (/ (+ c.r c.g c.b) 3))}
    :main
    ((=vec2 pos (/ gl_FragCoord.xy size))

     (=float frameParity (- (* (mod (float frame) 2) 2) 1))
     (=float pixelParity (- (* (mod (floor gl_FragCoord.x) 2) 2) 1))

     (=vec2 dir (vec2 1 0))
     (*= dir (* frameParity pixelParity))
     (= dir (/ dir size))

     (=vec4 currentValue (texture tex pos))
     (=vec4 comparisonValue (texture tex (+ pos dir)))

     (=float currentGrayscale (grayscale currentValue.rgb))
     (=float comparisonGrayscale (grayscale comparisonValue.rgb))

     (= fragColor
        (if (|| (< (+ pos.x dir.x) 0)
                (> (+ pos.x dir.x) 1))
          currentValue
          (if (< dir.x 0)
            (if (&& (> currentGrayscale threshold)
                    (> comparisonGrayscale currentGrayscale))
              comparisonValue
              currentValue)
            (if (&& (> comparisonGrayscale threshold)
                    (> currentGrayscale comparisonGrayscale))
              comparisonValue
              currentValue)))))})

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (run-purefrag-shader! gl
                          logic-frag-source
                          sort-resolution
                          {:floats {"size" [sort-resolution sort-resolution]
                                    "threshold" (first (mouse-pos))}
                           :textures {"tex" (first @texs-atom)}
                           :ints {"frame" @frame-atom}}
                          {:target (second @texs-atom)})
    (swap! texs-atom reverse)


    (square-maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          '{:version "300 es"
                            :precision {float highp}
                            :uniforms {size vec2
                                       tex sampler2D}
                            :outputs {fragColor vec4}
                            :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                   (= fragColor (texture tex pos)))}
                          resolution
                          {:floats {"size" resolution}
                           :textures {"tex" (first @texs-atom)}})

    (swap! frame-atom inc))
  (js/requestAnimationFrame update-page!))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (reset! texs-atom (u/gen 2 (create-f8-tex gl sort-resolution)))
    (reset! frame-atom 0)
    (run-purefrag-shader! gl
                          '{:version "300 es"
                            :precision {float highp}
                            :uniforms {size vec2
                                       tex sampler2D}
                            :outputs {fragColor vec4}
                            :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                   (= pos.y (- 1 pos.y))
                                   (= fragColor (texture tex pos)))}
                          sort-resolution
                          {:floats {"size" [sort-resolution sort-resolution]}
                           :textures {"tex"
                                      (html-image-texture gl "img")}}
                          {:target (first @texs-atom)}))
  (update-page!))
