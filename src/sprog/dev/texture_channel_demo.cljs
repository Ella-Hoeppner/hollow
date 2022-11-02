(ns sprog.dev.texture-channel-demo
  (:require [sprog.util :as u]
            [clojure.walk :refer [postwalk-replace]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.webgl.core :refer-macros [with-context]]))

(def texture-resolution 8)

(defonce gl-atom (atom nil))

(defonce texture-atom (atom nil))

(def render-frag-source
  (postwalk-replace
   {:texture-resolution-f (.toFixed texture-resolution 1)}
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor vec2}
     :main ((=vec2 pos (/ gl_FragCoord.xy :texture-resolution-f))
            (= fragColor (vec2 pos)))}))

(def draw-frag-source
  '{:version "300 es"
    :precision {float highp
                sampler2D highp}
    :uniforms {size vec2
               tex sampler2D}
    :outputs {fragColor vec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (=vec4 textureColor (texture tex pos))
           (= fragColor (vec4 textureColor.xy 0 1)))})

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas)
    (run-purefrag-shader! draw-frag-source
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)}
                           :textures {"tex" @texture-atom}})
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (reset! texture-atom (create-tex :f8
                                     texture-resolution
                                     {:filter-mode :nearest
                                      :channels 2}))
    (run-purefrag-shader! render-frag-source
                          texture-resolution
                          {}
                          {:target @texture-atom})
    (update-page!)))