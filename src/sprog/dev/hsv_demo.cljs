(ns sprog.dev.hsv-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [html-image-texture]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.colors :refer [hsv-to-rgb-chunk
                                              rgb-to-hsv-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]))

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

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          frag-source
                          resolution
                          {:floats {"size" resolution
                                    "mouse" (mouse-pos)}
                           :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (reset! tex-atom (html-image-texture gl "img" gl)))
  (update-page!))
