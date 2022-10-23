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
  (let [source-glsl
        (if (string? source)
          source
          (iglu->glsl source))
        shader (.createShader gl (or ({:frag gl.FRAGMENT_SHADER
                                       :vert gl.VERTEX_SHADER}
                                      shader-type)
                                     shader-type))]
    shader
    (.shaderSource gl shader source-glsl)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do (u/log (join "\n"
                       (map #(str %2 ":\t" %1)
                            (split-lines source-glsl)
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

(def purefrag-vert-glsl (iglu->glsl trivial-vert-source))

(defn create-purefrag-sprog [gl frag-source] 
  (let [{:keys [program] :as sprog}
        (create-sprog gl purefrag-vert-glsl frag-source)]
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

(defn run-sprog [gl sprog size uniform-map start length
                 & [{:keys [target offset]}]]
  (if target
    (if (coll? target)
      (apply (partial target-textures! gl) target)
      (target-textures! gl target))
    (target-screen! gl))
  (let [[width height] (if (number? size) [size size] size)
        [x y] (if offset offset [0 0])]
    (.viewport gl x y width height)
    (use-sprog gl sprog uniform-map)
    (.drawArrays gl gl.TRIANGLES start length)))

(defn run-purefrag-sprog [gl sprog size uniform-map & [options]]
  (run-sprog gl
             sprog
             size
             uniform-map
             0
             3
             options))

(defonce autosprog-cache-atom (atom {}))

(defn get-autosprog [gl shader-sources]
  (let [autosprog-key [gl shader-sources]]
    (if-let [autosprog (@autosprog-cache-atom autosprog-key)]
      autosprog
      (let [autosprog (apply (partial create-sprog gl) shader-sources)]
        (swap! autosprog-cache-atom assoc autosprog-key autosprog)
        autosprog))))

(defn run-autosprog [gl sources size uniform-map start length & [options]]
  (run-sprog gl 
             (get-autosprog gl sources)
             size
             uniform-map
             start
             length
             options))

(defonce purefrag-autosprog-cache-atom (atom {}))

(defn get-purefrag-autosprog [gl shader-source]
  (let [autosprog-key [gl shader-source]]
    (if-let [autosprog (@purefrag-autosprog-cache-atom autosprog-key)]
      autosprog
      (let [autosprog (create-purefrag-sprog gl shader-source)]
        (swap! purefrag-autosprog-cache-atom assoc autosprog-key autosprog)
        autosprog))))

(defn run-purefrag-autosprog [gl source size uniform-map & [options]]
  (run-purefrag-sprog gl 
                      (get-purefrag-autosprog gl source)
                      size
                      uniform-map
                      options))
