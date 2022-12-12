(ns sprog.dev.webcam-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-tex
                                          copy-html-image-data!
                                          create-webcam-video-element]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core :refer-macros [with-context]]))

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))
(defonce video-element-atom (atom nil))
(defonce time-updated?-atom (atom nil))

(with-context @gl-atom
  (defn update-page! []
    (maximize-gl-canvas {:square? true})
    (when @time-updated?-atom
      (copy-html-image-data! @tex-atom @video-element-atom))
    (run-purefrag-shader! '{:version "300 es"
                            :precision {float highp}
                            :uniforms {size vec2
                                       tex sampler2D}
                            :outputs {fragColor vec4}
                            :main ((=vec2 pos (/ gl_FragCoord.xy size))
                                   (= fragColor
                                      (vec4 (.xyz
                                             (texture tex
                                                      (vec2 pos.x (- 1 pos.y))))
                                            1)))}
                          (canvas-resolution)
                          {:floats {"size" (canvas-resolution)}
                           :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!))

  (defn init []
    (create-webcam-video-element
     (fn [video]
       (reset! gl-atom (create-gl-canvas true))
       (reset! tex-atom (create-tex :f8 1))
       (.addEventListener video "timeupdate" #(reset! time-updated?-atom true))
       (reset! video-element-atom video)
       (update-page!)))))
