(ns hollow.demos.raymarch
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [kudzu.chunks.raytracing :refer [ray-chunk
                                             sphere-intersection-chunk
                                             raymarch-chunk]]
            [kudzu.chunks.sdf :refer [sphere-sdf-chunk]]
            [kudzu.chunks.misc :refer [sigmoid-chunk
                                       gradient-chunk]]
            [kudzu.chunks.noise :refer [gabor-noise-chunk]]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.tools :refer [unquotable]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def sphere-radius 0.6)
(def max-distortion 0.1)

(def frag-glsl
  (unquotable
   (kudzu->glsl
    sphere-intersection-chunk
    sphere-sdf-chunk
    gabor-noise-chunk
    sigmoid-chunk
    gradient-chunk
    raymarch-chunk
    ray-chunk
    '{:precision {float highp}
      :uniforms {resolution vec2
                 time float}
      :outputs {frag-color vec4}
      :functions
      {sdf (float
            [x vec3]
            (=vec3 sphere-center (vec3 0 0 1))
            (+ (sd-sphere (- x sphere-center) ~sphere-radius)
               (* ~max-distortion
                  (-> (gabor-noise 4
                                  [3 4 5 6]
                                  (vec4 (- x sphere-center)
                                        (* 0.25 time)))
                      sigmoid
                      (* 2)
                      (- 1)))))}
      :main ((=Ray ray (Ray (vec3 0)
                            (normalize (vec3 (pixel-pos) 1))))
             (=vec2 bound-intersections
                    (find-sphere-intersections ray
                                             (vec3 0 0 1)
                                             ~(+ sphere-radius
                                                 (* max-distortion 1.01))))
             (=Ray bound-ray
                   (Ray (+ ray.pos (* ray.dir bound-intersections.x))
                        ray.dir))
             (=float surface-distance
                     (raymarch sdf
                               bound-ray
                               (- bound-intersections.y
                                  bound-intersections.x)
                               {:step-factor 0.65}))
             (= frag-color
                (vec4 (if (> surface-distance 0)
                        (-> (normalize
                             (find-gradient 3
                                            sdf
                                            0.0001
                                            (+ bound-ray.pos
                                               (* bound-ray.dir
                                                  surface-distance))))
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
     {:resolution (canvas-resolution)
      :time (u/seconds-since-startup)})
    {}))

(defn init []
  (js/window.addEventListener "load" #(start-hollow! nil update-page!)))
