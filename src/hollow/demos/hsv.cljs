(ns hollow.demos.hsv
  (:require [hollow.webgl.textures :refer [html-image-tex]]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            (hollow.input.mouse :refer [mouse-pos])
            [kudzu.chunks.color.hsv :refer [hsv->rgb-chunk
                                            rgb->hsv-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def frag-source
  (kudzu->glsl
   hsv->rgb-chunk
   rgb->hsv-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                mouse vec2
                tex sampler2D}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (=vec3 texRGB (.xyz (texture tex pos)))
            (=vec3 hsv (rgb->hsv texRGB))
            (= hsv.x (+ hsv.x (- (bi->uni mouse.x) 0.5)))
            (=vec3 outRGB (hsv->rgb hsv))
            (= frag-color (vec4 outRGB
                               1)))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
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
  (js/window.addEventListener "load" #(start-hollow! init-page! update-page!)))
