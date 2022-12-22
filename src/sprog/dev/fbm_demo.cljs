(ns sprog.dev.fbm-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.iglu.chunks.noise :refer [simplex-2d-chunk
                                             fbm-chunk]]
            [sprog.iglu.chunks.misc :refer [pos-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer [with-context]]))

(defonce gl-atom (atom nil))

(def frag-source
  (iglu->glsl
   pos-chunk
   simplex-2d-chunk
   fbm-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :main ((= fragColor
               (vec4 (vec3 (-> (fbm snoise2D
                                    2
                                    (* (getPos) 10)
                                    "3"
                                    4)
                               (+ 1)
                               (* 0.5)))
                     1)))}))

(defn update-page! []
  (with-context @gl-atom
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
