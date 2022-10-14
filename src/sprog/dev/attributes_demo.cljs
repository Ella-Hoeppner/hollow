(ns sprog.dev.attributes-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-sprog
                                         run-triangle-sprog]]
            [sprog.webgl.attributes :refer [set-buffer-data!
                                            set-sprog-attribute!]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def buffer-data [0 0
                  1 0
                  0 1])

(defonce gl-atom (atom nil))
(defonce buffer-atom (atom nil))
(defonce sprog-atom (atom nil))

(def vert-source
  (iglu->glsl
   '{:version "300 es"
     :precision {float highp}
     :inputs {vertexPos vec2}
     :signatures {main ([] void)}
     :functions {main ([] (= gl_Position (vec4 vertexPos 0 1)))}}))

(def frag-source
  (iglu->glsl
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main ([] (= fragColor (vec4 1 0 0 1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (set-sprog-attribute! @sprog-atom
                          "vertexPos"
                          @buffer-atom
                          2)
    (run-triangle-sprog @sprog-atom
                        resolution
                        {}
                        0
                        3)
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-sprog gl vert-source frag-source))
    (reset! buffer-atom (.createBuffer gl))
    (set-buffer-data! gl @buffer-atom (js/Float32Array. buffer-data)))
  (update-page!))
