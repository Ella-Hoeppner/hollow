(ns sprog.dev.multi-texture-output-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def texture-resolution 8)

(defonce gl-atom (atom nil))

(defonce texture-1-atom (atom nil))
(defonce texture-2-atom (atom nil))

(def render-frag-source
  (iglu->glsl
   {:texture-resolution-f (.toFixed texture-resolution 1)}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor0 vec4
               fragColor1 vec4}
     :qualifiers {fragColor0 "layout(location = 0)"
                  fragColor1 "layout(location = 1)"}
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

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          draw-frag-source
                          resolution
                          {:floats {"size" resolution}
                           :textures {"tex1" @texture-1-atom
                                      "tex2" @texture-2-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (doseq [tex-atom [texture-1-atom
                      texture-2-atom]]
      (reset! tex-atom
              (create-tex gl :f8 texture-resolution {:filter-mode :nearest})))
    (run-purefrag-shader! gl
                          render-frag-source
                          texture-resolution
                          {}
                          {:target [@texture-1-atom @texture-2-atom]}))
  (update-page!))
