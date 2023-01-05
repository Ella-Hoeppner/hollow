(ns sprog.dev.video-demo
  (:require [sprog.util :as u]
            [sprog.webgl.textures :refer [create-tex
                                          copy-html-image-data!]]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.webgl.core 
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(defn update-page! [{:keys [gl texture video] :as state}]
  (with-context gl
    (maximize-gl-canvas {:square? true})
    (copy-html-image-data! texture video)
    (run-purefrag-shader!
     '{:version "300 es"
       :precision {float highp}
       :uniforms {size vec2
                  tex sampler2D}
       :outputs {fragColor vec4}
       :main ((=vec2 pos (/ gl_FragCoord.xy size))
              (= fragColor
                 (texture tex
                          (vec2 pos.x (- 1 pos.y)))))}
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)}
      :textures {"tex" texture}}))
  state)

(defn init []
  (let [video (js/document.createElement "video")]
    (set! video.src "./test_video.mp4")
    (set! video.muted "muted")
    (set! video.loop "true")
    (.play video)
    (.addEventListener
     video
     "timeupdate"
     (let [started?-atom (atom false)]
       #(or @started?-atom
            (do (reset! started?-atom true)
                (start-sprog!
                 (fn [gl]
                   {:texture (create-tex gl :f8 1)
                    :video video})
                 update-page!)))))))
