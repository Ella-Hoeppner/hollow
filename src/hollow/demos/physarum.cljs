(ns hollow.demos.physarum
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [hollow.webgl.shaders :refer [run-shaders!
                                          run-purefrag-shader!]]
            [hollow.webgl.textures :refer [create-tex]]
            [kudzu.chunks.noise :refer [rand-chunk]]
            [kudzu.chunks.particles :refer [particle-vert-source
                                            particle-frag-source]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!]]))

(def substrate-resolution 1000)
(def agent-tex-resolution 100)

(def substrate-fade-factor 0.001)
(def substrate-spread-factor 0.01)

(def agent-radius 0.001)

(def sensor-distance 0.01)
(def sensor-spread 0.5)

(def agent-speed-factor 0.0001)
(def agent-turn-factor 0.1)

(def ambient-randomize-chance 0)

(def kudzu-wrapper
  (partial kudzu->glsl
           {:macros
            {:rand (fn [minimum maximum]
                     (+ minimum (rand (- maximum minimum))))}
            :constants
            {:u16-max (dec (Math/pow 2 16))
             :substrate-resolution substrate-resolution
             :agent-tex-resolution agent-tex-resolution
             :substrate-spread-factor substrate-spread-factor
             :substrate-fade-factor substrate-fade-factor
             :sensor-distance sensor-distance
             :sensor-spread sensor-spread
             :agent-speed-factor agent-speed-factor
             :agent-turn-factor agent-turn-factor
             :TAU u/TAU}}))

(def substrate-sample-chunk
  '{:precision {usampler2D highp}
    :uniforms {substrate usampler2D}
    :functions {substrateSample
                (float
                 [pos vec2]
                 (-> substrate
                     (texture pos)
                     .x
                     float
                     (/ :u16-max)))}})

(def render-frag-source
  (kudzu-wrapper
   substrate-sample-chunk
   '{:precision {float highp}
     :uniforms {resolution vec2}
     :outputs {frag-color vec4}
     :main ((=float sampleValue
                    (substrateSample (pixel-pos :uni)))
            (= frag-color (vec4 sampleValue
                               sampleValue
                               sampleValue
                               1)))}))

(def substrate-logic-frag-source
  (kudzu-wrapper
   substrate-sample-chunk
   '{:precision {float highp}
     :outputs {frag-color uvec4}
     :main
     ((=float centerSample (substrateSample (/ gl_FragCoord.xy
                                               :substrate-resolution)))
      (=float averageNeighborSample
              (/ (float
                  (+ (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 -1))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 0 -1))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 -1))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 0))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 0))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 1))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 0 1))
                                         :substrate-resolution))
                     (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 1))
                                         :substrate-resolution))))
                 8))
      (= frag-color
         (uvec4 (* (* (mix centerSample
                           averageNeighborSample
                           :substrate-spread-factor)
                      (- 1 :substrate-fade-factor))
                   :u16-max)
                0
                0
                0)))}))

(def agent-logic-frag-source
  (kudzu-wrapper
   rand-chunk
   substrate-sample-chunk
   '{:precision {float highp}
     :uniforms {agent-tex usampler2D
                randomize-chance float
                time float}
     :outputs {frag-color uvec4}
     :main
     ((=vec2 pos (/ gl_FragCoord.xy :agent-tex-resolution))
      (=uvec4 old-frag-color (texture agent-tex pos))

      (=vec2 agent-pos (/ (vec2 old-frag-color.xy) :u16-max))
      (=float agent-angle (* :TAU (/ (float old-frag-color.z) :u16-max)))

      (=float clockwise-angle (+ agent-angle :sensor-spread))
      (=float clockwise-sensor-sample
              (substrateSample
               (+ agent-pos
                  (* :sensor-distance
                     (vec2 (cos clockwise-angle)
                           (sin clockwise-angle))))))

      (=float counterclockwise-angle (- agent-angle :sensor-spread))
      (=float counterclockwise-sensor-sample
              (substrateSample
               (+ agent-pos
                  (* :sensor-distance
                     (vec2 (cos counterclockwise-angle)
                           (sin counterclockwise-angle))))))

      (=float newagent-angle
              (mod (+ agent-angle
                      (* :agent-turn-factor
                         (- clockwise-sensor-sample
                            counterclockwise-sensor-sample)))
                   :TAU))
      (=vec2 newagent-pos
             (mod (+ agent-pos
                     (* :agent-speed-factor
                        (vec2 (cos newagent-angle)
                              (sin newagent-angle))))
                  1))

      (=vec2 rand-seed (* 400 (+ pos (vec2 (mod time 3.217) 0))))

      (= frag-color
         (uvec4 (* (if (< (rand (+ rand-seed (:rand -100 100)))
                          randomize-chance)
                     (vec3 (rand (+ rand-seed (:rand -100 100)))
                           (rand (+ rand-seed (:rand -100 100)))
                           (rand (+ rand-seed (:rand -100 100))))
                     (vec3 newagent-pos
                           (/ newagent-angle :TAU)))
                   :u16-max)
                0)))}))

(defn update-agents! [randomize-chance
                      {:keys [gl substrate-textures agent-textures]
                       :as state}]
  (let [[front-tex back-tex] agent-textures
        substrate-tex (first substrate-textures)]
    (with-context gl
      (run-purefrag-shader! agent-logic-frag-source
                            agent-tex-resolution
                            {:randomize-chance randomize-chance
                             :time (u/seconds-since-startup)
                             :substrate substrate-tex
                             :agent-tex front-tex}
                            {:target back-tex})))
  (update state :agent-textures reverse))

(defn update-substrate! [{:keys [gl substrate-textures agent-textures]
                          :as state}]
  (let [[front-tex back-tex] substrate-textures
        agent-tex (first agent-textures)]
    (with-context gl
      (run-shaders! [(kudzu->glsl (particle-vert-source :u16))
                     (kudzu->glsl (particle-frag-source :u16))]
                    substrate-resolution
                    {:particle-tex agent-tex
                     :resolution substrate-resolution
                     :radius agent-radius}
                    {}
                    0
                    (* 6 agent-tex-resolution agent-tex-resolution)
                    {:target front-tex})

      (run-purefrag-shader! substrate-logic-frag-source
                            substrate-resolution
                            {:substrate front-tex}
                            {:target back-tex})))
  (update state :substrate-textures reverse))

(defn update-page! [{:keys [gl substrate-textures] :as state}]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (run-purefrag-shader! render-frag-source
                          (canvas-resolution)
                          {:resolution (canvas-resolution)
                           :substrate (first substrate-textures)}))
  (->> state
       (update-agents! ambient-randomize-chance)
       update-substrate!))

(defn init-page! [gl]
  (with-context gl
    (->> {:gl gl
          :substrate-textures
          (u/gen 2 (create-tex :u16 substrate-resolution))
          :agent-textures
          (u/gen 2 (create-tex :u16 agent-tex-resolution))}
         (update-agents! 1))))

(defn init []
  (start-hollow! init-page!
                 update-page!))