(ns sprog.dev.simple-gaussian-demo
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.postprocessing :refer [get-simple-gaussian]]
            [sprog.iglu.chunks.misc :refer [rescale-chunk]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [html-image-texture]]))

(defonce gl-atom (atom nil))
(defonce html-image-atom (atom nil))

(def frame-atom (atom 0))


(def frag-source
  (iglu->glsl
   {}
   rescale-chunk
   (get-simple-gaussian 32 2)
   '{:version "300 es"
     :precision {float highp
                 sampler2D highp}
     :outputs {fragColor vec4}
     :uniforms {size vec2
                time float
                tex sampler2D}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= pos.y (- 1 pos.y))
            (= fragColor (blur tex
                               pos
                               (rescale -1 1 0.01 0.9 (sin time)))))}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-canvas gl.canvas) 
    (run-purefrag-shader! gl
                          frag-source
                          resolution
                          {:floats {"size" resolution
                                    "time" (u/seconds-since-startup)}
                           :textures {"tex" @html-image-atom}})
    (swap! frame-atom inc)
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (maximize-canvas gl.canvas)
    (reset! html-image-atom (html-image-texture gl "img"))
    (reset! frame-atom 0)
    (update-page!)))

(defn ^:dev/after-load restart! []
  (js/document.body.removeChild (.-canvas @gl-atom))
  (init))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init)
                                       (update-page!))))


