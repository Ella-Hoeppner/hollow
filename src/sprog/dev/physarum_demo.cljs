(ns sprog.dev.physarum-demo
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas
                                      square-maximize-canvas]]
            [sprog.webgl.shaders :refer [run-shaders!
                                         run-purefrag-shader!]]
            [sprog.webgl.textures :refer [create-u16-tex]]
            [sprog.iglu.chunks.noise :refer [rand-chunk]]
            [sprog.iglu.chunks.particles :refer [particle-vert-source-u16
                                                 particle-frag-source-u16]]
            [sprog.iglu.core :refer [iglu->glsl]]))

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

(defonce gl-atom (atom nil))

(defonce substrate-texs-atom (atom nil))
(defonce agent-texs-atom (atom nil))

(defonce frame-atom (atom nil))

(def iglu-wrapper
  (partial iglu->glsl
           {:rand (fn [minimum maximum]
                    (+ minimum (rand (- maximum minimum))))
            :u16-max (dec (Math/pow 2 16))
            :substrate-resolution substrate-resolution
            :agent-tex-resolution agent-tex-resolution
            :substrate-spread-factor substrate-spread-factor
            :substrate-fade-factor substrate-fade-factor
            :sensor-distance sensor-distance
            :sensor-spread sensor-spread
            :agent-speed-factor agent-speed-factor
            :agent-turn-factor agent-turn-factor
            :TAU u/TAU}))

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

(defn update-agents! [randomize-chance]
  (let [gl @gl-atom
        [front-tex back-tex] @agent-texs-atom
        substrate-tex (first @substrate-texs-atom)]
    (run-purefrag-shader! gl
                          agent-logic-frag-source
                          agent-tex-resolution
                          {:floats {"randomizeChance" randomize-chance
                                    "time" @frame-atom}
                           :textures {"substrate" substrate-tex
                                      "agentTex" front-tex}}
                          {:target back-tex}))
  (swap! agent-texs-atom reverse))

(defn update-substrate! []
  (let [gl @gl-atom
        [front-tex back-tex] @substrate-texs-atom
        agent-tex (first @agent-texs-atom)]
    (run-shaders! gl
                  [particle-vert-source-u16 particle-frag-source-u16]
                  substrate-resolution
                  {:textures {"particleTex" agent-tex}
                   :floats {"size" substrate-resolution
                            "radius" agent-radius}}
                  {}
                  0
                  (* 6 agent-tex-resolution agent-tex-resolution)
                  {:target front-tex})

    (run-purefrag-shader! gl
                          substrate-logic-frag-source
                          substrate-resolution
                          {:textures {"substrate" front-tex}}
                          {:target back-tex}))
  (swap! substrate-texs-atom reverse))

(defn update-page! []
  (update-agents! ambient-randomize-chance)
  (update-substrate!)
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-canvas gl.canvas)
    (run-purefrag-shader! gl
                          render-frag-source
                          resolution
                          {:floats {"size" resolution}
                           :textures {"substrate" (first
                                                   @substrate-texs-atom)}}))
  (swap! frame-atom inc)
  (js/requestAnimationFrame update-page!))

(defn init []
  (let [gl (create-gl-canvas true)]
    (reset! gl-atom gl)

    (reset! substrate-texs-atom
            (u/gen 2 (create-u16-tex gl substrate-resolution)))
    (reset! agent-texs-atom
            (u/gen 2 (create-u16-tex gl agent-tex-resolution))))

  (reset! frame-atom 0)

  (update-agents! 1)

  (update-page!))
