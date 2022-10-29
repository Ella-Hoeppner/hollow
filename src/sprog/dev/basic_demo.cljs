(ns sprog.dev.basic-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [clojure.spec.alpha :as s]))

(def source
  #_'{:version "300 es"
      :precision {float highp}
      :uniforms {size vec2}
      :outputs {fragColor vec4}
      :signatures {getColor ([] vec2)}
      :functions {getColor ([] (/ gl_FragCoord.xy size))}
      :main ((=vec2 pos (/ gl_FragCoord.xy size))
             (= fragColor (vec4 (getColor) 0 1)))}
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2}
    :outputs {fragColor vec4}
    :functions {getColor
                {([] vec2)
                 ([] (/ gl_FragCoord.xy size))}
                main
                {([] void)
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (= fragColor (vec4 (getColor) 0 1)))}}})

(defonce gl-atom (atom nil))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          source
                          resolution
                          {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
