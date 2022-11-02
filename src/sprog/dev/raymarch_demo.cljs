(ns sprog.dev.raymarch-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.input.mouse :refer [mouse-pos
                                       mouse-present?]]
            [sprog.iglu.chunks.noise :refer [simplex-4d-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core :refer-macros [with-context]]))

(defonce gl-atom (atom nil))

(def frag-glsl
  (iglu->glsl
   {:raymarch-step-factor 0.5
    :max-ray-dist 5
    :fov 0.5
    :distortion-amplitude-factor 0.5
    :distortion-frequency-factor 4
    :time-factor 0.25
    :TAU u/TAU}
   simplex-4d-chunk
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size float
                time float
                mouse vec2}
     :outputs {fragColor vec4}
     :functions
     {distanceEstimate
      {([vec3] float)
       ([pos]
        (+ (* :distortion-amplitude-factor
              mouse.x
              (snoise4D (vec4 (* (mix 1 8 mouse.y) pos)
                              time)))
           (- (length pos) 1)))}
      rayNormal
      {([vec3 vec3] vec3)
       ([rayOrigin rayDirection]
        (=float rayLength 0)
        ("for(int i=0;i<512;i++)"
         (=vec3 rayPos (+ rayOrigin (* rayDirection rayLength)))
         (=float estimate (* :raymarch-step-factor (distanceEstimate rayPos)))
         ("if" (|| (< (abs estimate) 0.00001)
                   (> rayLength :max-ray-dist))
               "break")
         (+= rayLength estimate))
        (=vec3 finalRayPos (+ rayOrigin (* rayLength rayDirection)))
        (=vec2 e (vec2 0.00025 0))
        (if (> rayLength :max-ray-dist)
          (vec3 0)
          (normalize (vec3 (- (distanceEstimate (+ finalRayPos e.xyy))
                              (distanceEstimate (- finalRayPos e.xyy)))
                           (- (distanceEstimate (+ finalRayPos e.yxy))
                              (distanceEstimate (- finalRayPos e.yxy)))
                           (- (distanceEstimate (+ finalRayPos e.yyx))
                              (distanceEstimate (- finalRayPos e.yyx)))))))}}
     :main
     ((=vec2 pos (/ gl_FragCoord.xy size))

      (=float h (tan (/ :fov 2)))
      (=float viewportSize (* h 2))

      (=float focalLength 1)

      (=vec3 cameraOrigin (vec3 0 0 -5))
      (=vec3 cameraTarget (vec3 0 0 1))

      (=vec3 vup (vec3 0 1 0))

      (=vec3 w (normalize (- cameraOrigin cameraTarget)))
      (=vec3 u (normalize (cross vup w)))
      (=vec3 v (cross w u))

      (=vec3 horizontal (* u h 2))
      (=vec3 vertical (* v h 2))

      (=vec3 lowerLeftCorner (- cameraOrigin
                                (+ (* 0.5 horizontal)
                                   (* 0.5 vertical)
                                   w)))

      (=vec3 cameraDir (- (+ lowerLeftCorner
                             (* pos.x horizontal)
                             (* pos.y vertical))
                          cameraOrigin))
      (=vec3 surfaceNormal (rayNormal cameraOrigin cameraDir))

      (= fragColor
         (vec4 (* 0.5 (+ 1 surfaceNormal))
               1)))}))

(defn update-page! []
  (with-context @gl-atom
    (square-maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {:floats {"size" (first (canvas-resolution))
               "time" (u/seconds-since-startup)
               "mouse" (if (mouse-present?)
                         (mouse-pos)
                         [0 0])}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas true))
  (update-page!))
