(ns sprog.dev.texture-3d-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-f8-tex]]))

(def texture-data
  [255 0 0 255
   0 255 0 255
   0 0 255 255
   0 0 0 255
   0 255 0 255
   0 0 255 255
   0 0 0 255
   255 0 0 255])

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
  (reset! gl-atom (create-gl-canvas true))
  (reset! tex-atom
          (create-f8-tex @gl-atom
                         2
                         {:3d true
                          :data (js/Uint8Array. texture-data)}))
  (update-page!))
