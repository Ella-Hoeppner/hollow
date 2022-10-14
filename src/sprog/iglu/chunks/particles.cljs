(ns sprog.iglu.chunks.particles)

(def trivial-vert-source
  '{:version "300 es"
    :precision {float lowp}
    :inputs {vertPos vec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (= gl_Position vertPos))}})

(def particle-vert-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=int agentIndex (/ gl_VertexID 6))
      (=int corner "gl_VertexID % 6")

      (=ivec2 texSize (textureSize particleTex 0))

      (=vec2 texPos
             (/ (+ "0.5" (vec2 (% agentIndex texSize.x)
                               (/ agentIndex texSize.x)))
                (vec2 texSize)))

      (=uvec4 particleColor (texture particleTex texPos))
      (= particlePos (/ (vec2 particleColor.xy) "65535.0"))

      (= gl_Position
         (vec4 (- (* (+ particlePos
                        (* radius
                           (- (* "2.0"
                                 (if (|| (== corner 0)
                                         (== corner 3))
                                   (vec2 0 1)
                                   (if (|| (== corner 1)
                                           (== corner 4))
                                     (vec2 1 0)
                                     (if (== corner 2)
                                       (vec2 0 0)
                                       (vec2 1 1)))))
                              "1.0")))
                     "2.0")
                  "1.0")
               0
               1)))}})

(def particle-vert-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=int agentIndex (/ gl_VertexID 6))
      (=int corner "gl_VertexID % 6")

      (=ivec2 texSize (textureSize particleTex 0))

      (=vec2 texPos
             (/ (+ "0.5" (vec2 (% agentIndex texSize.x)
                               (/ agentIndex texSize.x)))
                (vec2 texSize)))

      (=uvec4 particleColor (texture particleTex texPos))
      (= particlePos (/ (vec2 particleColor.xy) "4294967295.0"))

      (= gl_Position
         (vec4 (- (* (+ particlePos
                        (* radius
                           (- (* "2.0"
                                 (if (|| (== corner 0)
                                         (== corner 3))
                                   (vec2 0 1)
                                   (if (|| (== corner 1)
                                           (== corner 4))
                                     (vec2 1 0)
                                     (if (== corner 2)
                                       (vec2 0 0)
                                       (vec2 1 1)))))
                              "1.0")))
                     "2.0")
                  "1.0")
               0
               1)))}})

(def particle-frag-source-u16
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (uvec4 65535 0 0 0)))}})

(def particle-frag-source-u32
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor uvec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (uvec4 65535 0 0 0)))}})

(def particle-frag-source-f8
  '{:version "300 es"
    :precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (vec4 1 0 0 1)))}})