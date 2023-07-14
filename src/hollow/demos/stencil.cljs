(ns hollow.demos.stencil
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-shaders!]]
            [hollow.webgl.core :refer [clear!
                                       enable!
                                       set-stencil-func!
                                       set-stencil-op!]]
            [hollow.webgl.attributes :refer [create-boj!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(defn render! [gl]
  (with-context gl
    (maximize-gl-canvas {:square? true})
    (let [draw-triangle! (fn [color coords]
                           (run-shaders!
                            ['{:precision {float highp}
                               :inputs {position vec2}
                               :main
                               ((= gl_Position
                                   (vec4 position
                                         0
                                         1)))}
                             '{:precision {float highp}
                               :uniforms {color vec3}
                               :outputs {fragColor vec4}
                               :main
                               ((= fragColor (vec4 color 1)))}]
                            (canvas-resolution)
                            {"color" color}
                            {"position"
                             (create-boj! 2 {:initial-data
                                             (js/Float32Array. coords)})}
                            0
                            3))]
      (clear! :all)
      (enable! :stencil-test)
      (set-stencil-func! :always 1)
      (set-stencil-op! :keep :keep :replace)
      (draw-triangle! [1 0 0]
                      [-0.1 0
                       0.1 0
                       0 0.1])
      (set-stencil-func! :equal)
      (set-stencil-op! :keep)
      (draw-triangle! [0 1 0]
                      [0 0.9
                       -0.9 -0.9
                       0.9 -0.9]))
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! render!
                                                     nil
                                                     {:stencil? true})))
