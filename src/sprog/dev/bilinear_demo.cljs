(ns sprog.dev.bilinear-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.chunks.misc :refer [bilinear-usampler-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer [with-context]]))

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

(def bilinear-frag-source
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

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas)
    (let [[width height] (canvas-resolution)
          half-width (* width 0.5)]
      (run-purefrag-shader! nearest-frag-source
                            [half-width height]
                            {:floats {"size" [half-width height]}
                             :textures {"tex" @tex-atom}})
      (run-purefrag-shader! bilinear-frag-source
                            [half-width 0 half-width height]
                            {:floats {"size" [half-width height]
                                      "offset" [half-width 0]}
                             :textures {"tex" @tex-atom}}))
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! tex-atom (create-tex :u16
                                 2
                                 {:wrap-mode :clamp
                                  :data (js/Uint16Array. pixel-data)}))
    (update-page!)))
