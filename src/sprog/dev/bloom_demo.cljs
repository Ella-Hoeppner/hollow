(ns sprog.dev.bloom-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-f8-tex
                                          copy-html-image-data!]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            (sprog.input.mouse :refer [mouse-pos])
            [sprog.iglu.chunks.misc :refer [get-bloom-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce tex-atom (atom nil))
(defonce video-element-atom (atom nil))
(defonce time-updated?-atom (atom nil))

(def frag-source
  (iglu->glsl
   nil
   (get-bloom-chunk :f8)
   '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2
               mouse vec2
               tex sampler2D}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions {main
                ([]
                 (=vec2 pos (/ gl_FragCoord.xy size))
                 (= pos.y (- 1 pos.y))
                 (= fragColor (bloom tex 
                                     pos 
                                     (mix 0 (/ 1 256) mouse.x)
                                     (- 1 mouse.y))))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-canvas gl.canvas)
    (when @time-updated?-atom
      (copy-html-image-data! gl @tex-atom @video-element-atom))
    (run-purefrag-shader! gl
                          frag-source
                          resolution
                          {:floats {"size" resolution
                                    "mouse" (mouse-pos)}
                           :textures {"tex" @tex-atom}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas true)
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
