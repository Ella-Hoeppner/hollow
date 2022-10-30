(ns sprog.dev.macro-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))

(def frag-source
  (iglu->glsl
   {:rand
    (fn
      ([] (rand))
      ([minimum maximum] (+ minimum (rand (- maximum minimum)))))}
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                time float
                mouse vec2}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (=float dist (distance pos
                                   (vec2 (:rand) (:rand))))
            (= fragColor
               (if (> dist (:rand))
                 (vec4 1)
                 (vec4 0 0 0 1))))}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [width height]]
    (square-maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          frag-source
                          resolution
                          {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
