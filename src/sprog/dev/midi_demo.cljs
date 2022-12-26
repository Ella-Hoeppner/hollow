(ns sprog.dev.midi-demo
  (:require
   [sprog.util :as u]
   [sprog.input.midi :refer [initialize-midi]]
   [sprog.iglu.core :refer [iglu->glsl]]
   [sprog.dom.canvas :refer [create-gl-canvas
                             maximize-gl-canvas
                             canvas-resolution]]
   [sprog.webgl.shaders :refer [run-purefrag-shader!]]
   [sprog.webgl.core :refer [with-context]] 
   [sprog.iglu.chunks.misc :refer [pos-chunk
                                   rescale-chunk]]
   [sprog.iglu.chunks.noise :refer [simplex-3d-chunk]]))

; state map atom and update fn, to organize and contain global state
(def state (atom {:smooth-factor 0.2}))
(def update-state! 
  #(swap! state merge %))

; WebGL context atom
(defonce gl-atom (atom nil))

; midi event callback
(defn midi-event-handler [message] 
(let [[command channel note velocity]
      message] 
 (update-state! {:note-target note
                 :velocity-target velocity})))

; "pure" fragment shader
(def frag-source 
  (iglu->glsl
   {}
   pos-chunk 
   rescale-chunk
   simplex-3d-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2
                frame float
                note float
                velocity float}
     :outputs {fragColor vec4} 
     :functions {circle 
                 {([vec2 vec2 float float] float)
                  ([pos circlePos radius smoothFactor]
                   (=float d (length (- pos circlePos)))
                   (=float t (smoothstep radius (- radius smoothFactor) d))
                   t)}} 
     :main ((=vec2 pos (getPos))
            (=float time (* frame 0.05)) 
            
            ; create background gradient
            (=vec3 background (mix (vec3 0.529 0.807 0.921) (vec3 1) pos.y))
            
            ; circle distance fn
            (=float circle (circle pos 
                                   ; here noise is added to cicle coordinates 
                                   ; with amplitude controlled by velocity
                                   (+ (vec2 0.5) 
                                      (* (snoise3D (vec3 (* 10 pos) time))
                                         (rescale 0 127 0 0.05 velocity)))
                                   ; radius controlled by midi note #
                                   (rescale 0 127 0.05 0.3 note)
                                   0.01))
            
            ; color inside circle
            (=vec3 circleCol (vec3 0.760 0.698 0.501))
            ; tweening background and circle color with distance 
            (=vec3 col (mix background circleCol circle))
            ; output to current pixel 
            (= fragColor (vec4 col 1)))}))

(defn update-page! []
  (with-context @gl-atom
    ; stretch canvas to fit window
    (maximize-gl-canvas)
    ; run shader frag-source, at canvas resolution, with uniforms in map
    (run-purefrag-shader!
     frag-source
     (canvas-resolution)
     {:floats {"size" (canvas-resolution)
               "frame" (:frame @state)
               "note" (:note @state)
               "velocity" (:velocity @state)}})
    ; updating global state, in this case lerping midi values
    ; and incementing time counter
    (update-state! {:frame (inc (:frame @state))
                    :note (u/scale (:note @state)
                                 (:note-target @state)
                                 (:smooth-factor @state))
                    :velocity (u/scale (:velocity @state)
                                    (:velocity-target @state)
                                    (:smooth-factor @state))}) 
    ; start/continue render loop
    (js/requestAnimationFrame update-page!)))

(defn init []
  ; creating WebGL context
  (reset! gl-atom (create-gl-canvas true))
  ; setting initial state
  (update-state! {:frame 0
                  :note 0
                  :note-target 127
                  :velocity 0
                  :velocity-target 127})
  ; initializing midi(will throw error on failure) and passing callback
  (initialize-midi midi-event-handler)
  ;start animation loop
  (update-page!))