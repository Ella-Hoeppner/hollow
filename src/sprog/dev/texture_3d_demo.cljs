(ns sprog.dev.texture-3d-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer [with-context]]))

(def tex-size 6)

(defonce gl-atom (atom nil))

(defonce tex-atom (atom nil))

(with-context @gl-atom
  (defn update-page! []
    (square-maximize-gl-canvas)
    (run-purefrag-shader! '{:version "300 es"
                            :precision {float highp
                                        sampler3D highp}
                            :uniforms {size vec2
                                       time float
                                       tex sampler3D}
                            :outputs {fragColor vec4}
                            :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                   (= fragColor
                                      (texture tex
                                               (vec3 pos (mod time 1)))))}
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)
                                    "time" (u/seconds-since-startup)}
                           :textures-3d {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!))

  (defn init []
    (reset! gl-atom (create-gl-canvas true))
    (let [tex (create-tex :f8
                          [tex-size tex-size 4]
                          {:wrap-mode [:clamp :clamp :repeat]})]
      (reset! tex-atom tex)
      (run-purefrag-shader! (iglu->glsl
                             {:tex-size-f (.toFixed tex-size 1)}
                             '{:version "300 es"
                               :precision {float highp
                                           sampler3D highp}
                               :outputs {layer1Color vec4
                                         layer2Color vec4
                                         layer3Color vec4
                                         layer4Color vec4}
                               :layout {layer1Color 0
                                        layer2Color 1
                                        layer3Color 2
                                        layer4Color 3}
                               :main
                               ((=float colorValue
                                        (/ (length
                                            (/ gl_FragCoord.xy :tex-size-f))
                                           (sqrt 2)))
                                (= layer1Color (vec4 colorValue 0 0 1))
                                (= layer2Color (vec4 0 colorValue 0 1))
                                (= layer3Color (vec4 0 0 colorValue 1))
                                (= layer4Color
                                   (vec4 colorValue colorValue colorValue 1)))})
                            tex-size
                            {}
                            {:target [[tex 0] [tex 1] [tex 2] [tex 3]]}))
    (update-page!)))