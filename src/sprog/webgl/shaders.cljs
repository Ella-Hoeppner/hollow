(ns sprog.webgl.shaders
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.particles :refer [trivial-vert-source]]
            [sprog.webgl.uniforms :refer [set-sprog-uniforms!]]
            [sprog.webgl.framebuffers :refer [target-textures!
                                              target-screen!]]
            [clojure.string :refer [split-lines
                                    join]]))

(defn create-shader [gl shader-type source]
  (let [shader (.createShader gl (or ({:frag gl.FRAGMENT_SHADER
                                       :vert gl.VERTEX_SHADER}
                                      shader-type)
                                     shader-type))]
    shader
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do (u/log (join "\n"
                       (map #(str %2 ":\t" %1)
                            (split-lines source)
                            (rest (range)))))
          (throw (js/Error. (str (.getShaderInfoLog gl shader))))))))

(defn create-program [gl vert-shader frag-shader]
  (let [program (.createProgram gl)]
    (.attachShader gl program vert-shader)
    (.attachShader gl program frag-shader)
    (.linkProgram gl program)
    (if (.getProgramParameter gl program gl.LINK_STATUS)
      program
      (throw (js/Error. (str (.getProgramInfoLog gl program)))))))

(defn create-sprog [gl vert-source frag-source]
  (let [program (create-program gl
                                (create-shader gl :vert vert-source)
                                (create-shader gl :frag frag-source))]
    {:program program
     :uniforms-atom (atom {})
     :attributes-atom (atom {})}))

(def purefrag-vert-source (iglu->glsl trivial-vert-source))

(defn create-purefrag-sprog [gl frag-source] 
  (let [{:keys [program] :as sprog}
        (create-sprog gl purefrag-vert-source frag-source)]
    (let [pos-buffer (.createBuffer gl)]
      (.bindBuffer gl
                   gl.ARRAY_BUFFER
                   pos-buffer)
      (.bufferData gl
                   gl.ARRAY_BUFFER
                   (js/Float32Array.
                    (clj->js [-1 -1
                              -1 3
                              3 -1]))
                   gl.STATIC_DRAW))
    (let [attrib (.getAttribLocation gl
                                     program
                                     "vertPos")]
      (.enableVertexAttribArray gl attrib)
      (.vertexAttribPointer gl
                            attrib
                            2
                            gl.FLOAT
                            false
                            0
                            0))
    sprog))

(defn use-sprog [gl {:keys [program] :as sprog} uniform-map]
  (.useProgram gl program)
  (set-sprog-uniforms! gl sprog uniform-map))

(defn run-sprog [gl
                 sprog
                 size
                 uniform-map
                 start
                 length
                 & [{:keys [targets offset]}]]
  (if targets
    (apply (partial target-textures! gl) targets)
    (target-screen! gl))
  (let [[width height] (if (number? size) [size size] size)
        [x y] (if offset offset [0 0])]
    (.viewport gl x y width height)
    (use-sprog gl sprog uniform-map)
    (.drawArrays gl gl.TRIANGLES start length)))

(defn run-purefrag-sprog [gl
                          sprog
                          size
                          uniform-map
                          & [options]]
  (run-sprog gl
             sprog
             size
             uniform-map
             0
             3
             options))
