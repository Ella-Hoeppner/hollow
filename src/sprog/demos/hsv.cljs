(ns sprog.demos.hsv
  (:require [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.kudzu.chunks.color :refer [hsv-to-rgb-chunk
                                              rgb-to-hsv-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def frag-source
  (kudzu->glsl
   hsv-to-rgb-chunk
   rgb-to-hsv-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                mouse vec2
                tex sampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec3 texRGB (.xyz (texture tex pos)))
            (=vec3 hsv (rgb2hsv texRGB))
            (= hsv.x (+ hsv.x (- mouse.x 0.5)))
            (=vec3 outRGB (hsv2rgb hsv))
            (= fragColor (vec4 outRGB
                               1)))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (maximize-gl-canvas {:square? true})
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "mouse" (mouse-pos)
                           "tex" texture}))
  state)

(defn init-page! [gl]
  {:texture (with-context gl
              (html-image-tex "img"))})

(defn init []
  (js/window.addEventListener "load" #(start-sprog! init-page! update-page!)))
