(ns sprog.dev.bloom-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [html-image-texture]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.postprocessing :refer [get-bloom-chunk
                                                      square-neighborhood]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))

(def frag-source
  (iglu->glsl
   nil
   (get-bloom-chunk :f8 (square-neighborhood 4 1) 5)
   '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2
               mouse vec2
               tex sampler2D}
    :outputs {fragColor vec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (= pos.y (- 1 pos.y))
           (= fragColor (-> (bloom tex
                                   pos
                                   (* mouse.x 0.0025)
                                   (- 1 mouse.y))
                            .xyz
                            (vec4 1))))}))

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
