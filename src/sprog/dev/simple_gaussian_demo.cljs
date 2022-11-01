(ns sprog.dev.simple-gaussian-demo
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.postprocessing 
             :refer
             [get-simple-gaussian-chunk]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.iglu.chunks.misc :refer [rescale-chunk]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [html-image-texture]]))

(defonce gl-atom (atom nil))
(defonce html-image-atom (atom nil))

(def frag-source
  (iglu->glsl
   {}
   rescale-chunk
   (get-simple-gaussian-chunk)
   '{:version "300 es"
     :precision {float highp
                 sampler2D highp}
     :outputs {fragColor vec4}
     :uniforms {size vec2
                mouse vec2
                tex sampler2D}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (= fragColor (blur tex
                               pos
                               (max 0.01 (* mouse.x 10)))))}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          frag-source
                          resolution
                          {:floats {"size" resolution
                                    "mouse" (mouse-pos)}
                           :textures {"tex" @html-image-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (reset! html-image-atom (html-image-texture gl "img"))
    (update-page!)))
