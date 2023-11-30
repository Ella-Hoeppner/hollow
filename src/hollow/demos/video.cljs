(ns hollow.demos.video
  (:require [hollow.util :as u]
            [hollow.webgl.textures :refer [create-tex
                                           copy-html-image-data!]]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(defn update-page! [{:keys [gl texture video] :as state}]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (copy-html-image-data! texture video)
    (run-purefrag-shader!
     '{:precision {float highp}
       :uniforms {size vec2
                  tex sampler2D}
       :outputs {frag-color vec4}
       :main ((=vec2 pos (/ gl_FragCoord.xy size))
              (= frag-color
                 (texture tex
                          (vec2 pos.x (- 1 pos.y)))))}
     (canvas-resolution)
     {"size" (canvas-resolution)
      "tex" texture}))
  state)

(defn init []
  (js/window.addEventListener
   "load"
   (fn []
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
                   (start-hollow!
                    (fn [gl]
                      {:texture (create-tex gl :f8 1)
                       :video video})
                    update-page!)))))))))
