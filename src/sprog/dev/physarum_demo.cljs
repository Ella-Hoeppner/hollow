(ns sprog.dev.physarum-demo
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        square-maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-sprog
                                         run-triangle-sprog
                                         create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!
                                              target-textures!]]
            [sprog.webgl.textures :refer [create-u16-tex]]
            [sprog.iglu.chunks.random :refer [rand-chunk]]
            [sprog.iglu.chunks.misc :refer [offset-shortcut]]
            [sprog.iglu.chunks.particles :refer [particle-vert-source-u16
                                                 particle-frag-source-u16]]
            [sprog.iglu.core :refer [iglu->glsl
                                     merge-chunks]]))

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
(defonce render-sprog-atom (atom nil))
(defonce substrate-logic-sprog-atom (atom nil))
(defonce agent-logic-sprog-atom (atom nil))
(defonce particle-sprog-atom (atom nil))

(defonce substrate-texs-atom (atom nil))
(defonce agent-texs-atom (atom nil))

(defonce fb-atom (atom nil))

(defonce frame-atom (atom nil))

(def bindings
  {:uint16-max-f (.toFixed (dec (Math/pow 2 16)) 1)
   :substrate-resolution-f (.toFixed substrate-resolution 1)
   :agent-tex-resolution-f (.toFixed agent-tex-resolution 1)
   :substrate-spread-factor (.toFixed substrate-spread-factor 8)
   :substrate-fade-factor (.toFixed substrate-fade-factor 8)
   :sensor-distance (.toFixed sensor-distance 8)
   :sensor-spread (.toFixed sensor-spread 8)
   :agent-speed-factor (.toFixed agent-speed-factor 8)
   :agent-turn-factor (.toFixed agent-turn-factor 8)
   :TAU (.toFixed u/TAU 8)})

(def replace-bindings (partial postwalk-replace bindings))

(def substrate-sample-chunk
  '{:precision {usampler2D highp}
    :uniforms {substrate usampler2D}
    :signatures {substrateSample ([vec2] float)}
    :functions {substrateSample
                ([pos]
                 (/ (float (.x (texture substrate pos)))
                    :uint16-max-f))}})

(def render-frag-source
  (replace-bindings
   (merge-chunks
    substrate-sample-chunk
    '{:version "300 es"
      :precision {float highp}
      :uniforms {size vec2}
      :outputs {fragColor vec4}
      :signatures {main ([] void)}
      :functions {main
                  ([]
                   (=float sampleValue
                           (substrateSample (/ gl_FragCoord.xy size)))
                   (= fragColor (vec4 sampleValue
                                      sampleValue
                                      sampleValue
                                      1)))}})))

(def substrate-logic-frag-source
  (replace-bindings
   (merge-chunks
    substrate-sample-chunk
    '{:version "300 es"
      :precision {float highp}
      :outputs {fragColor uvec4}
      :signatures {main ([] void)}
      :functions
      {main
       ([]
        (=float centerSample (substrateSample (/ gl_FragCoord.xy
                                                 :substrate-resolution-f)))
        (=float averageNeightborSample
                (/ (float
                    (+ (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 -1))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 0 -1))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 -1))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 0))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 0))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 -1 1))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 0 1))
                                           :substrate-resolution-f))
                       (substrateSample (/ (+ gl_FragCoord.xy (vec2 1 1))
                                           :substrate-resolution-f))))
                   "8.0"))

        (= fragColor
           (uvec4 (* (* (mix centerSample
                             averageNeightborSample
                             :substrate-spread-factor)
                        (- "1.0" :substrate-fade-factor))
                     :uint16-max-f)
                  0
                  0
                  0)))}})))

(def agent-logic-frag-source
  (offset-shortcut
   (replace-bindings
    (merge-chunks
     rand-chunk
     substrate-sample-chunk
     '{:version "300 es"
       :precision {float highp}
       :uniforms {agentTex usampler2D
                  randomizeChance float
                  time float}
       :outputs {fragColor uvec4}
       :signatures {main ([] void)}
       :functions
       {main
        ([]
         (=vec2 pos (/ gl_FragCoord.xy :agent-tex-resolution-f))
         (=uvec4 oldFragColor (texture agentTex pos))

         (=vec2 agentPos (/ (vec2 oldFragColor.xy) :uint16-max-f))
         (=float agentAngle (* :TAU (/ (float oldFragColor.z) :uint16-max-f)))

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
                     "1.0"))

         (=vec2 randSeed (* "400.0" (+ pos (vec2 (mod time "3.217") 0))))

         (= fragColor
            (uvec4 (* (if (< (rand [:offset randSeed]) randomizeChance)
                        (vec3 (rand [:offset randSeed])
                              (rand [:offset randSeed])
                              (rand [:offset randSeed]))
                        (vec3 newAgentPos
                              (/ newAgentAngle :TAU)))
                      :uint16-max-f)
                   0)))}}))))

(defn update-agents! [randomize-chance]
  (let [gl @gl-atom
        [front-tex back-tex] @agent-texs-atom
        substrate-tex (first @substrate-texs-atom)]
    (target-textures! gl @fb-atom back-tex)
    (run-purefrag-sprog @agent-logic-sprog-atom
                        agent-tex-resolution
                        {:floats {"randomizeChance" randomize-chance
                                  "time" @frame-atom}
                         :textures {"substrate" substrate-tex
                                    "agentTex" front-tex}}))
  (swap! agent-texs-atom reverse))

(defn update-substrate! []
  (let [gl @gl-atom
        [front-tex back-tex] @substrate-texs-atom
        agent-tex (first @agent-texs-atom)]
    (target-textures! gl @fb-atom front-tex)
    (run-triangle-sprog @particle-sprog-atom
                        substrate-resolution
                        {:textures {"particleTex" agent-tex}
                         :floats {"size" substrate-resolution
                                  "radius" agent-radius}}
                        0
                        (* 6 agent-tex-resolution agent-tex-resolution))

    (target-textures! gl @fb-atom back-tex)
    (run-purefrag-sprog @substrate-logic-sprog-atom
                        substrate-resolution
                        {:textures {"substrate" front-tex}}))
  (swap! substrate-texs-atom reverse))

(defn update-page! []
  (update-agents! ambient-randomize-chance)
  (update-substrate!)
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (square-maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @render-sprog-atom
                        resolution
                        {:floats {"size" resolution}
                         :textures {"substrate" (first @substrate-texs-atom)}}))
  (swap! frame-atom inc)
  (js/requestAnimationFrame update-page!))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! render-sprog-atom
            (create-purefrag-sprog
             gl
             (iglu->glsl render-frag-source)))
    (reset! substrate-logic-sprog-atom
            (create-purefrag-sprog
             gl
             (iglu->glsl substrate-logic-frag-source)))
    (reset! agent-logic-sprog-atom
            (create-purefrag-sprog
             gl
             (iglu->glsl agent-logic-frag-source)))
    (reset! particle-sprog-atom
            (create-sprog gl
                          (iglu->glsl particle-vert-source-u16)
                          (iglu->glsl particle-frag-source-u16)))

    (reset! substrate-texs-atom
            (u/gen 2 (create-u16-tex gl substrate-resolution)))
    (reset! agent-texs-atom
            (u/gen 2 (create-u16-tex gl agent-tex-resolution)))
    (reset! fb-atom (.createFramebuffer gl)))
  
  (reset! frame-atom 0)
  
  (update-agents! 1)
  
  (update-page!))
