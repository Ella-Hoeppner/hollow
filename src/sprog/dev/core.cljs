(ns sprog.dev.core
  (:require [sprog.util :as u]
            [sprog.canvas :refer [create-gl-canvas
                                  maximize-gl-canvas]]
            [sprog.shaders :refer [create-purefrag-sprog
                                   run-purefrag-sprog]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(def frag-source
  '{:version "300 es"
    :precision "highp float"
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
          width gl.canvas.width
          height gl.canvas.height]
      (.bindFramebuffer gl gl.FRAMEBUFFER nil)
      (run-purefrag-sprog sprog 
                          [width height] 
                          {:floats {"size" [width height]}}))
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
