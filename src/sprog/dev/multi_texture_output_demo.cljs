(ns sprog.dev.multi-texture-output-demo
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.textures :refer [create-f8-tex]]
            [sprog.webgl.framebuffers :refer [target-screen!
                                              target-textures!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def texture-resolution 8)

(defonce gl-atom (atom nil))

(defonce draw-sprog-atom (atom nil))

(defonce texture-1-atom (atom nil))
(defonce texture-2-atom (atom nil))

(def render-frag-source
  (postwalk-replace
   {:texture-resolution-f (.toFixed texture-resolution 1)}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor0 vec4
               fragColor1 vec4}
     :qualifiers {fragColor0 "layout(location = 0)"
                  fragColor1 "layout(location = 1)"}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy :texture-resolution-f))
                  (= fragColor0 (vec4 pos
                                      0
                                      1))
                  (= fragColor1 (vec4 0
                                      pos
                                      1)))}}))

(def draw-frag-source
  '{:version "300 es"
    :precision {float highp
                sampler2D highp}
    :uniforms {size vec2
               tex1 sampler2D
               tex2 sampler2D}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions {main
                ([]
                 (=vec2 pos (/ gl_FragCoord.xy size))
                 (= fragColor
                    (if (< pos.x "0.5")
                      (texture tex1 (* pos (vec2 "2.0" "1.0")))
                      (texture tex2 (* (- pos (vec2 "0.5" "0.0"))
                                      (vec2 "2.0" "1.0"))))))}})

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @draw-sprog-atom
                        resolution
                        {:floats {"size" resolution}
                         :textures {"tex1" @texture-1-atom
                                    "tex2" @texture-2-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! draw-sprog-atom (create-purefrag-sprog
                             gl
                             (iglu->glsl draw-frag-source)))
    (doseq [tex-atom [texture-1-atom
                      texture-2-atom]]
      (reset! tex-atom (create-f8-tex gl
                                      texture-resolution
                                      {:filter-mode :nearest})))
    (let [render-sprog (create-purefrag-sprog
                        gl
                        (u/log (iglu->glsl render-frag-source)))]
      (target-textures! gl
                        (.createFramebuffer gl)
                        @texture-1-atom
                        @texture-2-atom)
      (run-purefrag-sprog render-sprog
                          texture-resolution
                          {})))
  (update-page!))
