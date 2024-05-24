(ns hollow.demos.bilinear
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.textures :refer [create-tex]]
            [kudzu.chunks.misc :refer [bilinear-usampler-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def u16-max (dec (Math/pow 2 16)))

(def pixel-data
  [u16-max 0 0 u16-max
   0 u16-max 0 u16-max
   0 0 u16-max u16-max
   0 0 0 u16-max])

(def nearest-frag-source
  (kudzu->glsl
   {:constants {:u16-max-f (.toFixed u16-max 1)}}
   '{:precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                tex usampler2D}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ gl_FragCoord.xy size))
            (= frag-color (/ (vec4 (texture tex pos)) :u16-max-f)))}))

(def bilinear-frag-source
  (kudzu->glsl
   {:constants {:u16-max-f (.toFixed u16-max 1)}}
   bilinear-usampler-chunk
   '{:precision {float highp
                 usampler2D highp}
     :uniforms {size vec2
                offset vec2
                tex usampler2D}
     :outputs {frag-color vec4}
     :main ((=vec2 pos (/ (- gl_FragCoord.xy offset) size))
            (= frag-color (/ (texture-bilinear tex pos) :u16-max-f)))}))

(defn update-page! [{:keys [gl texture] :as state}]
  (with-context gl
    (maximize-gl-canvas)
    (let [[width height] (canvas-resolution)
          half-width (* width 0.5)]
      (run-purefrag-shader! nearest-frag-source
                            [half-width height]
                            {"size" [half-width height]
                             "tex" texture})
      (run-purefrag-shader! bilinear-frag-source
                            [half-width 0 half-width height]
                            {"size" [half-width height]
                             "offset" [half-width 0]
                             "tex" texture})))
  state)

(defn init-page! [gl]
  {:gl gl
   :texture (with-context gl
              (create-tex :u16
                          2
                          {:wrap-mode :clamp
                           :data (js/Uint16Array. pixel-data)}))})

(defn init []
  (start-hollow! init-page! update-page!))
