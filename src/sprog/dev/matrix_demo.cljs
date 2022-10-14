(ns sprog.dev.matrix-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [run-purefrag-sprog
                                         create-program
                                         create-sprog
                                         use-sprog
                                         run-triangle-sprog
                                         create-shader]]
            [sprog.input.keyboard :refer [add-key-callback]]
            [sprog.input.mouse :refer [mouse-pos]]
            [sprog.iglu.chunks :refer [merge-chunks
                                       simplex-2d-chunk
                                       simplex-3d-chunk
                                       get-fbm-chunk]]
            [sprog.webgl.textures :refer [html-image-texture]]
            [sprog.webgl.uniforms :refer [set-sprog-uniforms!]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl]]))

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))
(defonce vao-atom (atom nil))

(def rmat [0 1])

(def pos [0 0 -1
          0 0.9428 0.3333
          -0.8165 -0.4714 0.3333
          0.8165 -0.4714 0.3333])

(defonce time-atom (atom nil))

(def vert-source
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2 
               rmat mat3}
    :inputs {a_position vec4}
    :outputs {v_pos vec4}
    :signatures {main ([] void)}
    :functions
    {main ([]
           (=vec2 rotated_position (* a_position rmat))
           (= v_pos (vec4 rotated_position 0 1))
           (= gl_Position (vec4 rotated_position 0 1)))}})

(def frag-source
  '{:version "300 es"
    :precision {float highp}
    :uniforms {size vec2}
    :inputs {v_pos vec4}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions {main
                ([] 
                 (=vec2 pos (/ gl_FragCoord.xy size))
                 (= fragColor (vec4 pos 0 1)))}})

(defn rotation [angle-in-radians]
  (let [c (Math/cos angle-in-radians)
        s (Math/sin angle-in-radians)]
    [c (* s -1) 0
     s c 0
     0 0 1]))

(defn update-page! []
  (let [gl @gl-atom]
    (maximize-gl-canvas gl)
    (swap! time-atom inc)
    (set-sprog-uniforms! @sprog-atom {:floats {"time" @time-atom
                                                "size" [gl.canvas.width
                                                        gl.canvas.height]}
                                      :matrices {"rmat" rmat}})
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 1 1 1 1)
    (.clear gl gl.COLOR_BUFFER_BIT)

    #_(.bufferData gl
                   gl.ARRAY_BUFFER
                   (js/Float32Array.
                    (mapv #(- (* % 2) 1) (repeatedly 64 rand)))
                   gl.DYNAMIC_DRAW)
    (.useProgram gl (:program @sprog-atom))
    (.bindVertexArray gl @vao-atom)
    (.drawArrays gl
                 gl.TRIANGLES
                 0
                 4))
  (js/requestAnimationFrame update-page!))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! time-atom 0)

    (reset! sprog-atom (create-sprog
                        gl
                        (iglu->glsl 
                         {:zoom "500.0"
                          :octaves 5
                          :hurst 0.1}
                                    vert-source
                                    simplex-3d-chunk
                                    (get-fbm-chunk 'snoise 3))

                        (iglu->glsl {:octaves 1
                                     :hurst 0.99
                                     :zoom1 (rand 100)
                                     :zoom2 (rand 100)
                                     :zoom3 (rand 100)}
                                    frag-source
                                    simplex-3d-chunk
                                    (get-fbm-chunk 'snoise 3))))

    (.useProgram gl (:program @sprog-atom))
    (set-sprog-uniforms! @sprog-atom {:floats {"time" @time-atom
                                               "size" [gl.canvas.width 
                                                       gl.canvas.height]}
                                      :matrices {"rmat" (rotation 
                                                         (* @time-atom 0.01))}})
    (reset! vao-atom (.createVertexArray gl)))

  (let [gl @gl-atom
        attrib-location (.getAttribLocation gl (:program @sprog-atom) "a_position")
        position-buffer (.createBuffer gl)]
    (target-screen! gl)

    (.bindBuffer gl
                 gl.ARRAY_BUFFER
                 position-buffer)
    (.bufferData gl
                 gl.ARRAY_BUFFER
                 (js/Float32Array.
                  (mapv #(* % 0.5) pos))
                 gl.DYNAMIC_DRAW)
    (.bindVertexArray gl @vao-atom)
    (.enableVertexAttribArray gl attrib-location)
    (.vertexAttribPointer gl
                          attrib-location
                          3
                          gl.FLOAT
                          false
                          0
                          0))
  (add-key-callback " " update-page!)
  (update-page!))