(ns hollow.demos.pixel-sort
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.textures :refer [create-tex
                                           html-image-tex]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.input.mouse :refer [mouse-pos]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def sort-resolution 1000)

; adapted from https://www.shadertoy.com/view/wsSczw
(def logic-frag-source
  (kudzu->glsl
   '{:precision {float highp}
     :uniforms {size vec2
                tex sampler2D
                frame int
                threshold float}
     :outputs {frag-color vec4}
     :functions {grayscale
                 (float
                  [c vec3]
                  (/ (+ c.r c.g c.b) 3))}
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

      (= frag-color
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
               currentValue)))))}))


(defn update-page! [{:keys [gl frame textures] :as state}]
  (with-context gl
    (run-purefrag-shader! logic-frag-source
                          sort-resolution
                          {"size" [sort-resolution sort-resolution]
                           "threshold" (* 0.5 (inc (first (mouse-pos))))
                           "tex" (first textures)
                           "frame" frame}
                          {:target (second textures)})
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader! (kudzu->glsl
                           '{:precision {float highp}
                             :uniforms {size vec2
                                        tex sampler2D}
                             :outputs {frag-color vec4}
                             :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                    (= frag-color (texture tex pos)))})
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "tex" (second textures)}))
  (-> state
      (update :frame inc)
      (update :textures reverse)))

(defn init-page! [gl]
  (with-context gl
    (let [textures (u/gen 2 (create-tex :f8 sort-resolution))]
      (run-purefrag-shader! (kudzu->glsl
                             '{:precision {float highp}
                               :uniforms {size vec2
                                          tex sampler2D}
                               :outputs {frag-color vec4}
                               :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                      (= pos.y (- 1 pos.y))
                                      (= frag-color (texture tex pos)))})
                            sort-resolution
                            {"size" [sort-resolution sort-resolution]
                             "tex" (html-image-tex "img")}
                            {:target (first textures)})
      {:textures textures
       :frame 0})))

(defn init []
  (js/window.addEventListener "load"
                              #(start-hollow! init-page!
                                              update-page!)))
