(ns sprog.dev.raymarch-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-autosprog!]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.input.mouse :refer [mouse-pos
                                       mouse-present?]]
            [sprog.iglu.chunks.noise :refer [simplex-4d-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))

(def frag-glsl
  (iglu->glsl
   {:raymarch-step-factor "0.5"
    :max-ray-dist "5.0"
    :fov "0.5"
    :distortion-amplitude-factor "0.1"
    :distortion-frequency-factor "4.0"
    :time-factor "0.25"
    :TAU (.toFixed u/TAU 8)}
   simplex-4d-chunk
   '{:version "300 es"
     :precision {float highp
                 usampler2D highp}
     :uniforms {size float
                time float
                mouse vec2}
     :outputs {fragColor vec4}
     :signatures {distanceEstimate ([vec3] float)
                  rayNormal ([vec3 vec3] vec3)
                  main ([] void)}
     :functions
     {distanceEstimate
      ([pos]
       (+ (* :distortion-amplitude-factor
             mouse.x
             (snoise4D (vec4 (* (mix "1.0" "8.0" mouse.y) pos)
                             time)))
          (- (length pos) "1.0")))
      rayNormal
      ([rayOrigin rayDirection]
       (=float rayLength "0.0")
       ("for(int i=0;i<512;i++)"
        (=vec3 rayPos (+ rayOrigin (* rayDirection rayLength)))
        (=float estimate (* :raymarch-step-factor (distanceEstimate rayPos)))
        ("if" (|| (< (abs estimate) "0.00001")
                  (> rayLength :max-ray-dist))
              "break")
        (+= rayLength estimate))
       (=vec3 finalRayPos (+ rayOrigin (* rayLength rayDirection)))
       (=vec2 e (vec2 "0.00025" 0))
       (if (> rayLength :max-ray-dist)
         (vec3 0)
         (normalize (vec3 (- (distanceEstimate (+ finalRayPos e.xyy))
                             (distanceEstimate (- finalRayPos e.xyy)))
                          (- (distanceEstimate (+ finalRayPos e.yxy))
                             (distanceEstimate (- finalRayPos e.yxy)))
                          (- (distanceEstimate (+ finalRayPos e.yyx))
                             (distanceEstimate (- finalRayPos e.yyx)))))))
      main
      ([]
       (=vec2 pos (/ gl_FragCoord.xy size))

       (=float h (tan (/ :fov "2.0")))
       (=float viewportSize (* h "2.0"))

       (=float focalLength "1.0")

       (=vec3 cameraOrigin (vec3 0 0 -5))
       (=vec3 cameraTarget (vec3 0 0 1))

       (=vec3 vup (vec3 0 1 0))

       (=vec3 w (normalize (- cameraOrigin cameraTarget)))
       (=vec3 u (normalize (cross vup w)))
       (=vec3 v (cross w u))

       (=vec3 horizontal (* u h "2.0"))
       (=vec3 vertical (* v h "2.0"))

       (=vec3 lowerLeftCorner (- cameraOrigin
                                 (+ (* "0.5" horizontal)
                                    (* "0.5" vertical)
                                    w)))

       (=vec3 cameraDir (- (+ lowerLeftCorner
                              (* pos.x horizontal)
                              (* pos.y vertical))
                           cameraOrigin))
       (=vec3 surfaceNormal (rayNormal cameraOrigin cameraDir))

       (= fragColor
          (vec4 (* "0.5"
                   (+ "1.0"
                      surfaceNormal))
                1)))}}))

(defn update-page! []
  (let [gl @gl-atom
        resolution gl.canvas.width]
    (square-maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-autosprog! gl
                             frag-glsl
                             resolution
                             {:floats {"size" resolution
                                       "time" (u/seconds-since-startup)
                                       "mouse" (if (mouse-present?)
                                                 (mouse-pos)
                                                 [0 0])}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (reset! gl-atom (create-gl-canvas))
  (update-page!))
