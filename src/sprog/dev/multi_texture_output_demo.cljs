(ns sprog.dev.multi-texture-output-demo
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.textures :refer [create-float-tex]]
            [sprog.webgl.framebuffers :refer [target-screen!
                                              target-textures!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def texture-resolution 3)

(defonce gl-atom (atom nil))

(defonce draw-sprog-atom (atom nil))

(defonce texture-1-atom (atom nil))

(def render-frag-source
  (postwalk-replace
   {:texture-resolution-f (.toFixed texture-resolution 1)}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy :texture-resolution-f))
                  (= fragColor (vec4 pos
                                     0
                                     1)))}}))

(def draw-frag-source
  '{:version "300 es"
    :precision {float highp
                sampler2D highp}
    :uniforms {size vec2
               tex sampler2D}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions {main
                ([]
                 (= fragColor (texture tex (/ gl_FragCoord.xy size))))}})

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @draw-sprog-atom
                        resolution
                        {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! draw-sprog-atom (create-purefrag-sprog
                             gl
                             (iglu->glsl draw-frag-source)))
    (reset! texture-1-atom (create-float-tex gl 
                                             texture-resolution 
                                             {:filter-mode :nearest}))
    (let [render-sprog (create-purefrag-sprog
                        gl
                        (iglu->glsl render-frag-source))]
      (target-textures! gl (.createFramebuffer gl) @texture-1-atom)
      (run-purefrag-sprog render-sprog
                          texture-resolution
                          {})))
  (update-page!))
