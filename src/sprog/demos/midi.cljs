(ns sprog.demos.midi
  (:require
   [sprog.util :as u]
   [sprog.input.midi :refer [add-midi-callback]]
   [kudzu.core :refer [kudzu->glsl]]
   [sprog.dom.canvas :refer [maximize-gl-canvas
                             canvas-resolution]]
   [sprog.webgl.shaders :refer [run-purefrag-shader!]]
   [kudzu.chunks.misc :refer [pos-chunk
                                    rescale-chunk]]
   [kudzu.chunks.noise :refer [simplex-3d-chunk]]
   [sprog.webgl.core
    :refer-macros [with-context]
    :refer [start-sprog!]]))

(def smooth-factor 0.2)

(defonce circle-radius-atom (atom 0))
(defonce circle-radius-target-atom (atom 1))
(defonce noise-scale-atom (atom 0))
(defonce noise-scale-target-atom (atom 1))

; midi event callback
(defn midi-event-handler [{:keys [note velocity]}]
  (reset! circle-radius-target-atom (/ note 127))
  (reset! noise-scale-target-atom (/ velocity 127)))

; shader source
(def frag-glsl
  (kudzu->glsl
   pos-chunk
   rescale-chunk
   simplex-3d-chunk
   '{:precision {float highp}
     :uniforms {size vec2
                time float
                circleRadius float
                noiseScale float}
     :outputs {fragColor vec4}
     :main ((=vec2 pos (getPos))

            ; determine if current pixel is inside or outside noisey circle
            (=float circleValue
                    (->> pos
                         ; find signed distance to circle at center of screen
                         (- (vec2 0.5))
                         length
                         (- circleRadius)
                         ; add offset to distance with noise function
                         (+ (* noiseScale (snoise3D (vec3 (* 10 pos) time))))
                         ; separate inside and outside with smoothstep
                         (smoothstep 0 0.01)))

            ; define colors
            (=vec3 background (mix (vec3 0.529 0.807 0.921) (vec3 1) pos.y))
            (=vec3 circleInterior (vec3 0.760 0.698 0.501))

            ; tween background and circle color
            (=vec3 col (mix background circleInterior circleValue))

            ; output to pixel 
            (= fragColor (vec4 col 1)))}))

(defn render [gl]
  (with-context gl
    ; stretch canvas to fit window
    (maximize-gl-canvas)
    ; run shader, at canvas resolution, with uniforms in map
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)
      "time" (u/seconds-since-startup)
      "circleRadius" (u/scale 0.05 0.3 @circle-radius-atom)
      "noiseScale" (u/scale 0 0.05 @noise-scale-atom)})))

(defn update-states! []
  (swap! circle-radius-atom
         #(u/scale % @circle-radius-target-atom smooth-factor))
  (swap! noise-scale-atom
         #(u/scale % @noise-scale-target-atom smooth-factor)))

(defn update-page! [{:keys [gl]}]
  (render gl)
  (update-states!))

(defn init []
  (js/window.addEventListener
   "load"
   (fn []
     ; initialize midi and register midi event callback
     (add-midi-callback midi-event-handler)

     ; start update loop
     (start-sprog! nil update-page!))))
