(ns sprog.dev.hsv-demo
  (:require #_[sprog.util :as u]
            [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.colors :refer [hsv-to-rgb-chunk
                                              rgb-to-hsv-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer [with-context]]))

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))

(def frag-source
  (iglu->glsl
   nil
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

(with-context @gl-atom
  (defn update-page! []
    (square-maximize-gl-canvas)
    (run-purefrag-shader! frag-source
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)
                                    "mouse" (mouse-pos)}
                           :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! tex-atom (html-image-tex "img"))

    (update-page!)))
