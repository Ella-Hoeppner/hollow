(ns sprog.dev.texture-3d-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-f8-tex]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(def tex-size 6)

(defonce gl-atom (atom nil))

(defonce tex-atom (atom nil))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          '{:version "300 es"
                            :precision {float highp
                                        sampler3D highp}
                            :uniforms {size vec2
                                       time float
                                       tex sampler3D}
                            :outputs {fragColor vec4}
                            :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                   (= fragColor
                                      (texture tex 
                                               (vec3 pos (mod time "1.0")))))}
                          resolution
                          {:floats {"size" resolution
                                    "time" (u/seconds-since-startup)}
                           :textures-3d {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)
        tex (create-f8-tex gl [tex-size tex-size 4] {:3d true
                                                     :wrap-mode :clamp})]
    (reset! gl-atom  gl)
    (reset! tex-atom tex)
    (run-purefrag-shader! gl
                          (iglu->glsl
                           {:tex-size-f (.toFixed tex-size 1)}
                           '{:version "300 es"
                             :precision {float highp
                                         sampler3D highp}
                             :outputs {layer1Color vec4
                                       layer2Color vec4
                                       layer3Color vec4
                                       layer4Color vec4}
                             :qualifiers {layer1Color "layout(location = 0)"
                                          layer2Color "layout(location = 1)"
                                          layer3Color "layout(location = 2)"
                                          layer4Color "layout(location = 3)"}
                             :main
                             ((=float colorValue
                                      (/ (length 
                                          (/ gl_FragCoord.xy :tex-size-f))
                                         (sqrt "2.0")))
                              (= layer1Color (vec4 colorValue 0 0 1))
                              (= layer2Color (vec4 0 colorValue 0 1))
                              (= layer3Color (vec4 0 0 colorValue 1))
                              (= layer4Color
                                 (vec4 colorValue colorValue colorValue 1)))})
                          tex-size
                          {}
                          {:target [[tex 0] [tex 1] [tex 2] [tex 3]]}))
  (update-page!))
