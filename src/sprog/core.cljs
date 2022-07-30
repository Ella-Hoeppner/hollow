(ns sprog.core
  (:require [iglu.core :refer [iglu->glsl]]))

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
      (throw (js/Error. (str (.getShaderInfoLog gl shader)))))))

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
     :gl gl
     :uniforms-atom (atom {})}))

(def purefrag-vert-source
  (iglu->glsl
   {:version "300 es"
    :precision "lowp float"
    :inputs '{vertPos vec4}
    :signatures '{main ([] void)}
    :functions
    '{main
      ([]
       (= gl_Position vertPos))}}))

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

(defn use-sprog [{:keys [gl program]}]
  (.useProgram gl program))

(defn ensure-uniform-present! [{:keys [gl program uniforms-atom]}
                               uniform-name-str]
  (when (not (@uniforms-atom uniform-name-str))
    (swap! uniforms-atom
           assoc
           uniform-name-str
           (.getUniformLocation gl
                                program
                                uniform-name-str))))

(defn set-sprog-uniform-1i! [{:keys [gl uniforms-atom] :as sprog}
                             uniform-name
                             value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform1i gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-1f! [{:keys [gl uniforms-atom] :as sprog}
                             uniform-name
                             value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform1f gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-2fv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform2fv gl (@uniforms-atom uniform-name-str) value)))
