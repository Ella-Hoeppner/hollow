(ns sprog.dev.bloom-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [html-image-tex]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.postprocessing :refer [get-bloom-chunk
                                                      square-neighborhood]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer-macros [with-context]]))

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

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas {:square? true})
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
