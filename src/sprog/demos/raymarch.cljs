(ns sprog.demos.raymarch
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.kudzu.chunks.raytracing :refer [ray-chunk
                                                   sphere-intersection-chunk
                                                   raymarch-chunk]]
            [sprog.kudzu.chunks.sdf :refer [sphere-sdf-chunk]]
            [sprog.kudzu.chunks.misc :refer [pos-chunk
                                             sigmoid-chunk
                                             gradient-chunk]]
            [sprog.kudzu.chunks.noise :refer [gabor-noise-chunk]]
            [sprog.kudzu.core :refer [kudzu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def sphere-radius 0.6)
(def max-distortion 0.1)

(def frag-glsl
  (u/unquotable
   (kudzu->glsl
    pos-chunk
    sphere-intersection-chunk
    sphere-sdf-chunk
    gabor-noise-chunk
    sigmoid-chunk
    gradient-chunk
    raymarch-chunk
    ray-chunk
    '{:precision {float highp}
      :uniforms {size vec2
                 time float}
      :outputs {fragColor vec4}
      :functions
      {sdf (float
            [x vec3]
            (=vec3 sphereCenter (vec3 0 0 1))
            (+ (sdSphere x sphereCenter ~sphere-radius)
               (* ~max-distortion
                  (-> (gaborNoise 4
                                  [3 4 5 6]
                                  (vec4 (- x sphereCenter)
                                        (* 0.25 time)))
                      sigmoid
                      (* 2)
                      (- 1)))))}
      :main ((=vec2 pos (getPos))
             (=Ray ray (Ray (vec3 0)
                            (-> pos
                                (* 2)
                                (- 1)
                                (vec3 1)
                                normalize)))
             (=vec2 boundIntersections
                    (findSphereIntersections ray
                                             (vec3 0 0 1)
                                             ~(+ sphere-radius
                                                 (* max-distortion 1.01))))
             (=Ray boundRay
                   (Ray (+ ray.pos (* ray.dir boundIntersections.x))
                        ray.dir))
             (=float surfaceDistance
                     (raymarch sdf
                               boundRay
                               (- boundIntersections.y
                                  boundIntersections.x)
                               {:step-factor 0.65}))
             (= fragColor
                (vec4 (if (> surfaceDistance 0)
                        (-> (normalize
                             (findGradient 3
                                           sdf
                                           0.0001
                                           (+ boundRay.pos
                                              (* boundRay.dir
                                                 surfaceDistance))))
                            (+ 1)
                            (* 0.5))
                        (vec3 0))
                      1)))})))

(defn update-page! [{:keys [gl]}]
  (with-context gl
    (maximize-gl-canvas)
    (run-purefrag-shader!
     frag-glsl
     (canvas-resolution)
     {"size" (canvas-resolution)
      "time" (u/seconds-since-startup)})))

(defn init []
  (js/window.addEventListener "load" #(start-sprog! nil update-page!)))
