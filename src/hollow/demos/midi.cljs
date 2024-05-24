(ns hollow.demos.midi
  (:require
   [hollow.util :as u]
   [hollow.input.midi :refer [add-note-down-callback
                              add-note-up-callback
                              down-notes]]
   [kudzu.core :refer [kudzu->glsl]]
   [hollow.dom.canvas :refer [maximize-gl-canvas
                              canvas-resolution]]
   [hollow.webgl.shaders :refer [run-purefrag-shader!]]
   [kudzu.tools :refer [unquotable]]
   [hollow.webgl.core
    :refer-macros [with-context]
    :refer [start-hollow!
            update-hollow-state!]]))

(def circle-count 64)

(def frag-glsl
  (unquotable
   (kudzu->glsl
    '{:precision {float highp}
      :uniforms {resolution vec2
                 time float
                 circles [vec2 ~(str circle-count)]}
      :outputs {frag-color vec4}
      :main ((= frag-color (vec4 0 0 0 1))
             (:for [i ~circle-count]
                   (:when (< (distance (pixel-pos :uni) [circles i]) 0.1)
                          (= frag-color (vec4 1)))))})))

(defn update-page! [{:keys [gl circles] :as state}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader! frag-glsl
                          (canvas-resolution)
                          {:resolution (canvas-resolution)
                           :circles (flatten
                                     (take circle-count
                                           (concat (vals circles)
                                                   (repeat [-10 -10]))))
                           :time (u/seconds-since-startup)}))
  state)

(defn init []
  (add-note-down-callback
   (fn [{:keys [note]}]
     (update-hollow-state! #(update % :circles assoc note [(rand) (rand)]))))
  (add-note-up-callback
   (fn [{:keys [note]}]
     (update-hollow-state! #(update % :circles dissoc note))))
  (start-hollow! nil update-page!))
