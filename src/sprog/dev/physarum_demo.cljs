(ns sprog.dev.physarum-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [maximize-gl-canvas
                                      canvas-resolution]]
            [sprog.webgl.shaders :refer [run-shaders!
                                         run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-tex]]
            [sprog.iglu.chunks.noise :refer [rand-chunk]]
            [sprog.iglu.chunks.particles :refer [particle-vert-source-u16
                                                 particle-frag-source-u16]]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.webgl.core
             :refer-macros [with-context]
             :refer [start-sprog!]]))

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

(def iglu-wrapper
  (partial iglu->glsl
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
                {([vec2] float)
                 ([pos]
                  (-> substrate
                      (texture pos)
                      .x
                      float
                      (/ :u16-max)))}}})

(def render-frag-source
  (iglu-wrapper
   substrate-sample-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {size vec2}
     :outputs {fragColor vec4}
     :main ((=float sampleValue
                    (substrateSample (/ gl_FragCoord.xy size)))
            (= fragColor (vec4 sampleValue
                               sampleValue
                               sampleValue
                               1)))}))

(def substrate-logic-frag-source
  (iglu-wrapper
   substrate-sample-chunk
   '{:version "300 es"
     :precision {float highp}
     :outputs {fragColor uvec4}
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
      (= fragColor
         (uvec4 (* (* (mix centerSample
                           averageNeighborSample
                           :substrate-spread-factor)
                      (- 1 :substrate-fade-factor))
                   :u16-max)
                0
                0
                0)))}))

(def agent-logic-frag-source
  (iglu-wrapper
   rand-chunk
   substrate-sample-chunk
   '{:version "300 es"
     :precision {float highp}
     :uniforms {agentTex usampler2D
                randomizeChance float
                time float}
     :outputs {fragColor uvec4}
     :main
     ((=vec2 pos (/ gl_FragCoord.xy :agent-tex-resolution))
      (=uvec4 oldFragColor (texture agentTex pos))

      (=vec2 agentPos (/ (vec2 oldFragColor.xy) :u16-max))
      (=float agentAngle (* :TAU (/ (float oldFragColor.z) :u16-max)))

      (=float clockwiseAngle (+ agentAngle :sensor-spread))
      (=float clockwiseSensorSample
              (substrateSample
               (+ agentPos
                  (* :sensor-distance
                     (vec2 (cos clockwiseAngle)
                           (sin clockwiseAngle))))))

      (=float counterclockwiseAngle (- agentAngle :sensor-spread))
      (=float counterclockwiseSensorSample
              (substrateSample
               (+ agentPos
                  (* :sensor-distance
                     (vec2 (cos counterclockwiseAngle)
                           (sin counterclockwiseAngle))))))

      (=float newAgentAngle
              (mod (+ agentAngle
                      (* :agent-turn-factor
                         (- clockwiseSensorSample
                            counterclockwiseSensorSample)))
                   :TAU))
      (=vec2 newAgentPos
             (mod (+ agentPos
                     (* :agent-speed-factor
                        (vec2 (cos newAgentAngle)
                              (sin newAgentAngle))))
                  1))

      (=vec2 randSeed (* 400 (+ pos (vec2 (mod time 3.217) 0))))

      (= fragColor
         (uvec4 (* (if (< (rand (+ randSeed (:rand -100 100)))
                          randomizeChance)
                     (vec3 (rand (+ randSeed (:rand -100 100)))
                           (rand (+ randSeed (:rand -100 100)))
                           (rand (+ randSeed (:rand -100 100))))
                     (vec3 newAgentPos
                           (/ newAgentAngle :TAU)))
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
                           {"randomizeChance" randomize-chance
                            "time" (u/seconds-since-startup)
                            "substrate" substrate-tex
                            "agentTex" front-tex}
                           {:target back-tex})))
  (update state :agent-textures reverse))

(defn update-substrate! [{:keys [gl substrate-textures agent-textures]
                          :as state}]
  (let [[front-tex back-tex] substrate-textures
        agent-tex (first agent-textures)]
    (with-context gl
      (run-shaders! [particle-vert-source-u16 particle-frag-source-u16]
                    substrate-resolution
                    {"particleTex" agent-tex
                     "size" substrate-resolution
                     "radius" agent-radius}
                    {}
                    0
                    (* 6 agent-tex-resolution agent-tex-resolution)
                    {:target front-tex})

      (run-purefrag-shader! substrate-logic-frag-source 
                            substrate-resolution
                            {"substrate" front-tex}
                            {:target back-tex})))
  (update state :substrate-textures reverse))

(defn update-page! [{:keys [gl substrate-textures] :as state}]
  (with-context gl
    (maximize-gl-canvas {:square? true})
    (run-purefrag-shader! render-frag-source
                          (canvas-resolution)
                          {"size" (canvas-resolution)
                           "substrate" (first substrate-textures)}))
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
  (start-sprog! init-page!
                update-page!))
