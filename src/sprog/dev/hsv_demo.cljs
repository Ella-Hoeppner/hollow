(ns sprog.dev.hsv-demo
  (:require [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.colors :refer [hsv-to-rgb-chunk
                                              rgb-to-hsv-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer [with-context
                                      start-update-loop!]]))

(def frag-source
  (iglu->glsl
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
                          {:floats {"size" (canvas-resolution)
                                    "mouse" (mouse-pos)}
                           :textures {"tex" texture}}))
  state)

(defn init []
  (let [gl (create-gl-canvas true)]
    (start-update-loop! update-page!
                        {:gl gl
                         :texture (with-context gl
                                    (html-image-tex "img"))})))
