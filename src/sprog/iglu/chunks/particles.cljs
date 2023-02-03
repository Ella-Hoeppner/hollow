(ns sprog.iglu.chunks.particles
  (:require [sprog.util :as u]))

(def particle-vert-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float} 
    :main ((=int agentIndex (/ gl_VertexID i6))
           (=int corner (% gl_VertexID i6))

           (=ivec2 texSize (textureSize particleTex i0))

           (=vec2 texPos
                  (/ (+ 0.5 (vec2 (% agentIndex texSize.x)
                                  (/ agentIndex texSize.x)))
                     (vec2 texSize)))

           (=uvec4 particleColor (texture particleTex texPos))
           (= particlePos (/ (vec2 particleColor.xy) 65535))

           (= gl_Position
              (vec4 (- (* (+ particlePos
                             (* radius
                                (- (* 2
                                      (if (|| (== corner i0)
                                              (== corner i3))
                                        (vec2 0 1)
                                        (if (|| (== corner i1)
                                                (== corner i4))
                                          (vec2 1 0)
                                          (if (== corner i2)
                                            (vec2 0 0)
                                            (vec2 1 1)))))
                                   1)))
                          2)
                       1)
                    0
                    1)))})

(def particle-vert-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :main ((=int agentIndex (/ gl_VertexID i6))
           (=int corner (% gl_VertexID i6))

           (=ivec2 texSize (textureSize particleTex i0))

           (=vec2 texPos
                  (/ (+ 0.5 (vec2 (% agentIndex texSize.x)
                                  (/ agentIndex texSize.x)))
                     (vec2 texSize)))

           (=uvec4 particleColor (texture particleTex texPos))
           (= particlePos (/ (vec2 particleColor.xy) 4294967295))

           (= gl_Position
              (vec4 (- (* (+ particlePos
                             (* radius
                                (- (* 2
                                      (if (|| (== corner i0)
                                              (== corner i3))
                                        (vec2 0 1)
                                        (if (|| (== corner i1)
                                                (== corner i4))
                                          (vec2 1 0)
                                          (if (== corner i2)
                                            (vec2 0 0)
                                            (vec2 1 1)))))
                                   1)))
                          2)
                       1)
                    0
                    1)))})

(def particle-frag-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (=float dist (distance pos particlePos))
           ("if" (> dist radius)
                 "discard")
           (= fragColor (uvec4 65535 0 0 0)))})

(def particle-frag-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (=float dist (distance pos particlePos))
           ("if" (> dist radius)
                 "discard")
           (= fragColor (uvec4 65535 0 0 0)))})

(def particle-frag-source-f8
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor vec4}
    :main ((=vec2 pos (/ gl_FragCoord.xy size))
           (=float dist (distance pos particlePos))
           ("if" (> dist radius)
                 "discard")
           (= fragColor (vec4 1 0 0 1)))})

 (def particle-vert-3d-source-u32
   '{:uniforms {time float
                radius float
                perspective mat4
                particleTex usampler2D
                cubeDistance float}
     :outputs {squarePos vec2
               vertexPos vec3}
     :main
     ((=int particleIndex (/ gl_VertexID i6))
      (=int corner (% gl_VertexID i6))

      (= squarePos
         (vec2 (if (|| (== corner "0")
                       (== corner "3")
                       (== corner "2"))
                 -1
                 1)
               (if (|| (== corner "1")
                       (== corner "4")
                       (== corner "2"))
                 -1
                 1)))

      (=ivec2 texSize (textureSize particleTex i0))

      (=uvec4 particleTexColor
              (texelFetch particleTex
                          (ivec2 (% particleIndex texSize.x)
                                 (/ particleIndex texSize.x))
                          "0"))
      (=vec3 particlePos
             (-> particleTexColor
                 .xyz
                 vec3
                 (/ ~(dec (Math/pow 2 32)))
                 (* 2)
                 (- 1)))

      (= vertexPos
         (vec3 (+ particlePos.xy
                  (* radius
                     squarePos))
               (- particlePos.z
                  (+ 1 cubeDistance))))

      (= gl_Position (* (vec4 vertexPos 1)
                        perspective))
      (= gl_Position (/ gl_Position gl_Position.w)))})

(def particle-frag-3d-source-u32
  '{:uniforms {size vec2
               radius float
               lightPos vec3
               ambientLight float}
    :inputs {vertexPos vec3
             squarePos vec2}
    :outputs {fragColor vec4}
    :main
    ((=float horizontalDist (length squarePos))
     ("if" (> horizontalDist 1) "discard")
     (=float depthDist (sqrt (- 1 (* horizontalDist horizontalDist))))
     (=vec3 surfacePos
            (- vertexPos
               (vec3 0 0 (* radius depthDist))))
     (=vec3 surfaceNormal (vec3 squarePos depthDist))
     (=vec3 lightDiff (normalize (- lightPos surfacePos)))
     (=float lightFactor (mix ambientLight
                              1
                              (max 0 (dot surfaceNormal lightDiff))))
     (= fragColor (vec4 lightFactor 1)))})
