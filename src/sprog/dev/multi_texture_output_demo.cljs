(ns sprog.dev.multi-texture-output-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer-macros [with-context]]))

(def texture-resolution 8)

(defonce gl-atom (atom nil))

(defonce texture-1-atom (atom nil))
(defonce texture-2-atom (atom nil))

(def render-frag-source
  (iglu->glsl
   {:constants {:texture-resolution-f (.toFixed texture-resolution 1)}}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor0 vec4
               fragColor1 vec4}
     :layout {fragColor0 0
              fragColor1 1}
     :main ((=vec2 pos (/ gl_FragCoord.xy :texture-resolution-f))
            (= fragColor0 (vec4 pos
                                0
                                1))
            (= fragColor1 (vec4 0
                                pos
                                1)))}))

(def draw-frag-source
  '{:version "300 es"
    :precision {float highp
                sampler2D highp}
    :uniforms {size vec2
               tex1 sampler2D
               tex2 sampler2D}
    :outputs {fragColor vec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (= fragColor
              (if (< pos.x 0.5)
                (texture tex1 (* pos (vec2 2 1)))
                (texture tex2 (* (- pos (vec2 0.5 0))
                                 (vec2 2 1))))))})

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas)
    (run-purefrag-shader!
     draw-frag-source
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)}
      :textures {"tex1" @texture-1-atom
                 "tex2" @texture-2-atom}})
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (doseq [tex-atom [texture-1-atom
                      texture-2-atom]]
      (reset! tex-atom
              (create-tex :f8 texture-resolution {:filter-mode :nearest})))
    (run-purefrag-shader! render-frag-source
                          texture-resolution
                          {}
                          {:target [@texture-1-atom @texture-2-atom]})
    (update-page!)))
