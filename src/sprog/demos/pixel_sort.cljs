(ns sprog.demos.pixel-sort
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex
                                          html-image-tex]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def sort-resolution 1000)

; adapted from https://www.shadertoy.com/view/wsSczw
(def logic-frag-source
  (kudzu->glsl
   '{:precision {float highp}
     :uniforms {size vec2
                tex sampler2D
                frame int
                threshold float}
     :outputs {fragColor vec4}
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
               currentValue)))))}))


(defn update-page! [{:keys [gl frame textures] :as state}]
  (with-context gl
    (run-purefrag-shader! logic-frag-source
                          sort-resolution
                          {"size" [sort-resolution sort-resolution]
                           "threshold" (first (mouse-pos))
                           "tex" (first textures)
                           "frame" frame}
                          {:target (second textures)})
    (maximize-gl-canvas {:square? true})
    (run-purefrag-shader! (kudzu->glsl
                           '{:precision {float highp}
                             :uniforms {size vec2
                                        tex sampler2D}
                             :outputs {fragColor vec4}
                             :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                    (= fragColor (texture tex pos)))})
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
                               :outputs {fragColor vec4}
                               :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                      (= pos.y (- 1 pos.y))
                                      (= fragColor (texture tex pos)))})
                            sort-resolution
                            {"size" [sort-resolution sort-resolution]
                             "tex" (html-image-tex "img")}
                            {:target (first textures)})
      {:textures textures
       :frame 0})))

(defn init []
  (js/window.addEventListener "load"
                              #(start-sprog! init-page!
                                             update-page!)))
