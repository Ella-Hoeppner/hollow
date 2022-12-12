(ns sprog.dev.vertex-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-shaders!]]
            [sprog.webgl.attributes :refer [create-boj!]]
            [sprog.webgl.core :refer-macros [with-context]]))

(def pos-buffer-data [0 0
                      1 0
                      0 1])

(def color-buffer-data [1 0 0
                        0 1 0
                        0 0 1])

(defonce gl-atom (atom nil))

(defonce pos-boj-atom (atom nil))
(defonce color-boj-atom (atom nil))

(def vert-source
  '{:version "300 es"
    :precision {float highp}
    :inputs {vertexPos vec2
             vertexColor vec3}
    :outputs {color vec3}
    :uniforms {rotation mat2}
    :main ((= color vertexColor)
           (= gl_Position (vec4 (* vertexPos rotation) 0 1)))})

(def frag-source
  '{:version "300 es"
    :precision {float highp}
    :inputs {color vec3}
    :outputs {fragColor vec4}
    :main ((= fragColor (vec4 color 1)))})

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas {:square? true})
    (run-shaders! [vert-source frag-source]
                  (canvas-resolution)
                  {:matrices {"rotation"
                              (let [angle (u/seconds-since-startup)]
                                [(Math/cos angle) (- (Math/sin angle))
                                 (Math/sin angle) (Math/cos angle)])}}
                  {"vertexPos" @pos-boj-atom
                   "vertexColor" @color-boj-atom}
                  0
                  3)
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! pos-boj-atom
            (create-boj! 2
                         {:initial-data (js/Float32Array. pos-buffer-data)}))
    (reset! color-boj-atom
            (create-boj! 3
                         {:initial-data (js/Float32Array. color-buffer-data)}))
    (update-page!)))
