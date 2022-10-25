(ns sprog.dev.texture-3d-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-f8-tex]]))

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
                                      (texture tex (vec3 pos time))))}
                          resolution
                          {:floats {"size" resolution
                                    "time" (u/seconds-since-startup)}
                           :textures-3d {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)
        tex (create-f8-tex gl 2 {:3d true})]
    (reset! gl-atom  gl)
    (reset! tex-atom tex)
    (run-purefrag-shader! gl
                          '{:version "300 es"
                            :precision {float highp
                                        sampler3D highp}
                            :outputs {layer1Color vec4
                                      layer2Color vec4}
                            :qualifiers {layer1Color "layout(location = 0)"
                                         layer2Color "layout(location = 1)"}
                            :main
                            ((= layer1Color (vec4 (/ gl_FragCoord.xy
                                                     "2.0")
                                                  0
                                                  1))
                             (= layer2Color (vec4 0
                                                  (/ gl_FragCoord.xy)
                                                  1)))}
                          2
                          {}
                          {:target [[tex 0] [tex 1]]}))
  (update-page!))
