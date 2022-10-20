(ns sprog.dev.webcam-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-f8-tex
                                          copy-html-image-data!
                                          create-webcam-video-element]]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.chunks.colors :refer [hsv-to-rgb-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))
(defonce tex-atom (atom nil))
(defonce video-element-atom (atom nil))
(defonce time-updated?-atom (atom nil))

(def frag-source
  (iglu->glsl
   {}
   hsv-to-rgb-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                tex sampler2D}
     :outputs {fragColor vec4}
     :signatures {main ([] void)}
     :functions {main
                 ([]
                  (=vec2 pos (/ gl_FragCoord.xy size))
                  (= fragColor (vec4 (.xyz (texture tex
                                                    (vec2 pos.x
                                                          (- "1.0" pos.y))))
                                     1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-gl-canvas gl)
    (when @time-updated?-atom
      (copy-html-image-data! gl @tex-atom @video-element-atom))
    (target-screen! gl)
    (run-purefrag-sprog @sprog-atom
                        resolution
                        {:floats {"size" resolution}
                         :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (create-webcam-video-element
   (fn [video]
     (let [gl (create-gl-canvas)]
       (reset! gl-atom gl)
       (reset! tex-atom (create-f8-tex gl 1))
       (reset! sprog-atom (create-purefrag-sprog gl frag-source))
       (.addEventListener video "timeupdate" #(reset! time-updated?-atom true))
       (reset! video-element-atom video)
       (update-page!)))))
