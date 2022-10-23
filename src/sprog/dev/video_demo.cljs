(ns sprog.dev.video-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-f8-tex
                                          copy-html-image-data!]]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]))

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))
(defonce video-element-atom (atom nil))
(defonce time-updated?-atom (atom nil))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-gl-canvas gl)
    (when @time-updated?-atom
      (copy-html-image-data! gl @tex-atom @video-element-atom))
    (run-purefrag-shader! gl
                          '{:version "300 es"
                            :precision {float highp}
                            :uniforms {size vec2
                                       tex sampler2D}
                            :outputs {fragColor vec4}
                            :signatures {main ([] void)}
                            :functions
                            {main
                             ([]
                              (=vec2 pos (/ gl_FragCoord.xy size))
                              (= fragColor
                                 (texture tex
                                          (vec2 pos.x
                                                (- "1.0" pos.y)))))}}
                          resolution
                          {:floats {"size" resolution}
                           :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)
        video (js/document.createElement "video")]
    (set! video.src "./test_video.mp4")
    (set! video.muted "muted")
    (set! video.loop "true")
    (.play video)
    (.addEventListener video "timeupdate" #(reset! time-updated?-atom true))
    (reset! video-element-atom video)

    (reset! gl-atom gl)
    (reset! tex-atom (create-f8-tex gl 1)))
  (update-page!))
