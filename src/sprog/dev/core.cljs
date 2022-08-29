(ns sprog.dev.core
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                  maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                   run-purefrag-sprog]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(def frag-source
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions {main
                ([]
                 (=vec2 pos (/ gl_FragCoord.xy size))
                 (= fragColor (vec4 pos 0 1)))}})

(defn update-page! []
  (let [gl @gl-atom]
    (maximize-gl-canvas gl)
    (let [sprog @sprog-atom
          resolution [gl.canvas.width gl.canvas.height]]
      (.bindFramebuffer gl gl.FRAMEBUFFER nil)
      (run-purefrag-sprog sprog 
                          resolution
                          {:floats {"size" resolution}}))
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-purefrag-sprog
                        gl
                        (iglu->glsl frag-source))))
  (update-page!))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
