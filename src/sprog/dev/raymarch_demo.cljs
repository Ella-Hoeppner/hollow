(ns sprog.dev.raymarch-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-purefrag-shader!]]
            [sprog.iglu.chunks.raytracing :refer [ray-chunk
                                                  sphere-intersection-chunk
                                                  raymarch-chunk]]
            [sprog.iglu.chunks.sdf :refer [sphere-sdf-chunk
                                           blob-sdf-chunk
                                           octahedron-sdf-chunk]]
            [sprog.iglu.chunks.misc :refer [pos-chunk
                                            sigmoid-chunk
                                            gradient-chunk]]
            [sprog.iglu.chunks.noise :refer [gabor-noise-chunk]]
            [sprog.iglu.core :refer [iglu->glsl]] 
            [sprog.iglu.chunks.transformations :refer [x-rotation-matrix-chunk
                                                       y-rotation-matrix-chunk]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

(def sphere-radius 0.6)
(def max-distortion 0.1)

(def frag-glsl
  (u/unquotable
   (iglu->glsl
    pos-chunk
    y-rotation-matrix-chunk
    x-rotation-matrix-chunk
    sphere-intersection-chunk
    sphere-sdf-chunk
    octahedron-sdf-chunk
    blob-sdf-chunk
    gabor-noise-chunk
    sigmoid-chunk
    gradient-chunk
    raymarch-chunk
    ray-chunk
    '{:version "300 es"
      :precision {float highp}
      :uniforms {size vec2
                 time float
                 mouse vec2}
      :outputs {fragColor vec4}
      :functions
      {rotationMatrix
       {([] mat3)
        ([]
         (=vec2 mouseControl (-> mouse 
                                 (* 2)
                                 (- 1)))
         (* (yRotationMatrix mouseControl.x)
            (xRotationMatrix mouseControl.y)))}
       sdf {([vec3] float)
            ([x]
             (*= x (rotationMatrix))
             (+ (sdSphere x (vec3 0) ~sphere-radius)
                (* ~max-distortion
                   (-> (gaborNoise 4
                                   [3 4 5 6]
                                   (vec4 x
                                         (* 0.25 time)))
                       sigmoid
                       (* 2)
                       (- 1)))))}}
      :main ((=vec2 pos (getPos))
             (=Ray ray (Ray (vec3 0 0 -1)
                            (-> pos
                                (* 2)
                                (- 1)
                                (vec3 1)
                                normalize)))
             (=vec2 boundIntersections
                    (findSphereIntersections ray
                                             (vec3 0 0 0)
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
                               {:step-factor 0.15}))
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
     {:floats {"size" (canvas-resolution)
               "time" (u/seconds-since-startup)
               "mouse" (mouse-pos)}})))

(defn init []
  (start-sprog! nil update-page!))
