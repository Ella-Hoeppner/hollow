(ns sprog.dev.attributes-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-sprog
                                         run-triangle-sprog]]
            [sprog.webgl.attributes :refer [create-boj!
                                            set-boj-data!
                                            set-sprog-attribute!]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def pos-buffer-data [0 0
                      1 0
                      0 1])

(def color-buffer-data [1 0 0
                        0 1 0
                        0 0 1])

(defonce gl-atom (atom nil))
(defonce pos-boj-atom (atom nil))
(defonce color-boj-atom (atom nil))
(defonce sprog-atom (atom nil))

(def vert-source
  (iglu->glsl
   '{:version "300 es"
     :precision {float highp}
     :inputs {vertexPos vec2
              vertexColor vec3}
     :outputs {color vec3}
     :signatures {main ([] void)}
     :functions {main ([] 
                       (= color vertexColor)
                       (= gl_Position (vec4 vertexPos 0 1)))}}))

(def frag-source
  (iglu->glsl
   '{:version "300 es"
     :precision {float highp}
     :inputs {color vec3}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main ([] (= fragColor (vec4 color 1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (set-sprog-attribute! @sprog-atom "vertexPos" @pos-boj-atom)
    (set-sprog-attribute! @sprog-atom "vertexColor" @color-boj-atom)
    (run-triangle-sprog @sprog-atom resolution {} 0 3)
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-sprog gl vert-source frag-source))
    (reset! pos-boj-atom (create-boj! gl 2))
    (reset! color-boj-atom (create-boj! gl 3))
    (set-boj-data! gl @pos-boj-atom (js/Float32Array. pos-buffer-data))
    (set-boj-data! gl @color-boj-atom (js/Float32Array. color-buffer-data)))
  (update-page!))
