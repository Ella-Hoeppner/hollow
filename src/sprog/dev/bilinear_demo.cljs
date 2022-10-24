(ns sprog.dev.bilinear-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-u16-tex]]
            [sprog.iglu.chunks.misc :refer [bilinear-usampler-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def u16-max (dec (Math/pow 2 16)))

(def pixel-data
  [u16-max 0 0 u16-max
   0 u16-max 0 u16-max
   0 0 u16-max u16-max
   0 0 0 u16-max])

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))

(def nearest-frag-source
  (iglu->glsl
   {:u16-max-f (.toFixed u16-max 1)}
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                tex usampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= fragColor (/ (vec4 (texture tex pos)) :u16-max-f)))}))

(def bicubic-frag-source
  (iglu->glsl
   {:u16-max-f (.toFixed u16-max 1)}
   bilinear-usampler-chunk
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                offset vec2
                tex usampler2D}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (/ (- gl_FragCoord.xy offset) size))
            (= fragColor (/ (textureBilinear tex pos) :u16-max-f)))}))

(defn update-page! []
  (let [gl @gl-atom
        width gl.canvas.width
        height gl.canvas.height
        resolution [width height]
        half-resolution (update resolution 0 (partial * 0.5))]
    (maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          nearest-frag-source
                          half-resolution
                          {:floats {"size" half-resolution}
                           :textures {"tex" @tex-atom}})
    (run-purefrag-shader! gl
                          bicubic-frag-source
                          half-resolution
                          {:floats {"size" half-resolution
                                    "offset" [(* width 0.5) 0]}
                           :textures {"tex" @tex-atom}}
                          {:offset [(* width 0.5) 0]})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)
    (reset! tex-atom (create-u16-tex gl 2 {:wrap-mode :clamp
                                           :data (js/Uint16Array. pixel-data)})))
  (update-page!))
