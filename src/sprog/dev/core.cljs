(ns sprog.dev.core
  (:require [sprog.util :as u]
            [sprog.canvas :refer [create-gl-canvas
                                  maximize-gl-canvas]]
            [sprog.shaders :refer [create-purefrag-sprog
                                   use-sprog
                                   set-sprog-float-uniforms!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(defn update-page! []
  (let [gl @gl-atom]
    (maximize-gl-canvas gl)
    (let [sprog @sprog-atom
          width gl.canvas.width
          height gl.canvas.height]
      (.bindFramebuffer gl gl.FRAMEBUFFER nil)
      (.viewport gl 0 0 width height)
      (use-sprog sprog)
      (set-sprog-float-uniforms! sprog {"size" [width height]})
      (.drawArrays gl gl.TRIANGLES 0 3))
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom
            (create-purefrag-sprog
             gl
             (iglu->glsl
              '{:version "300 es"
                :precision "highp float"
                :uniforms {size vec2}
                :outputs {fragColor vec4}
                :signatures {main ([] void)}
                :functions {main
                            ([]
                             (=vec2 pos (/ gl_FragCoord.xy size))
                             (= fragColor (vec4 pos 0 1)))}}))))
  (update-page!))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
