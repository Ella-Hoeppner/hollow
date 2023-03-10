(ns sprog.webgl.shaders
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]
            [sprog.iglu.chunks.misc :refer [trivial-vert-source]]
            [sprog.webgl.uniforms :refer [set-sprog-uniforms!]]
            [sprog.webgl.textures :refer [target-textures!
                                          target-screen!]]
            [sprog.webgl.attributes :refer [set-sprog-attributes!
                                            set-sprog-attribute!
                                            create-boj!]]
            [clojure.string :refer [split-lines
                                    join]]))

(defn create-shader [gl shader-type source-glsl]
  (let [shader (.createShader gl (or ({:frag gl.FRAGMENT_SHADER
                                       :vert gl.VERTEX_SHADER}
                                      shader-type)
                                     shader-type))]
    (.shaderSource gl shader source-glsl)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do (u/log
           (let [lines (split-lines source-glsl)
                 digit-count (count (str (count lines)))]
             (join "\n"
                   (map #(str (apply str (take (inc digit-count)
                                               (concat (str %2) 
                                                       (list ":")
                                                       (repeat " "))))
                              " "
                              %1)
                        lines
                        (rest (range))))))
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
  (let [vert-glsl (if (string? vert-source)
                    vert-source
                    (iglu->glsl vert-source))
        frag-glsl (if (string? frag-source)
                    frag-source
                    (iglu->glsl frag-source))
        program (create-program gl
                                (create-shader gl :vert vert-glsl)
                                (create-shader gl :frag frag-glsl))]
    {:program program
     :uniform-locations-atom (atom {})
     :attribute-locations-atom (atom {})
     :uniform-type-map
     (into {}
           (map #(-> %
                     (clojure.string/replace #"uniform\s+" "")
                     (clojure.string/split #"\s+")
                     reverse
                     vec)
                (re-seq #"uniform\s+[A-Za-z0-9]+\s+[A-Za-z0-9]+"
                        (str vert-glsl
                             "\n"
                             frag-glsl))))}))

(def purefrag-vert-glsl (iglu->glsl trivial-vert-source))

(defonce purefrag-vert-pos-bojs-atom (atom {}))

(defn purefrag-vert-pos-boj [gl]
  (when (not (@purefrag-vert-pos-bojs-atom gl))
    (swap! purefrag-vert-pos-bojs-atom
           assoc
           gl
           (create-boj! gl
                        2
                        {:initial-data (js/Float32Array.
                                        (clj->js [-1 -1
                                                  -1 3
                                                  3 -1]))})))
  (@purefrag-vert-pos-bojs-atom gl))

(defn create-purefrag-sprog [gl frag-source] 
  (let [sprog (create-sprog gl purefrag-vert-glsl frag-source)]
    (set-sprog-attribute! gl
                          sprog
                          "vertPos"
                          (purefrag-vert-pos-boj gl))
    sprog))

(defn use-sprog! [gl {:keys [program] :as sprog} uniform-map attribute-map]
  (.useProgram gl program)
  (set-sprog-uniforms! gl sprog uniform-map)
  (set-sprog-attributes! gl sprog attribute-map))

(defn run-sprog! [gl sprog size uniform-map attribute-map start length
                  & [{:keys [target]}]]
  (if target
    (if (coll? target)
      (apply (partial target-textures! gl) target)
      (target-textures! gl target))
    (target-screen! gl))
  (let [[offset-x offset-y width height]
        (cond (number? size) [0 0 size size]
              (= (count size) 2) (vec (concat [0 0] size))
              (= (count size) 4) (vec size))]
    (.viewport gl offset-x offset-y width height)
    (use-sprog! gl sprog uniform-map attribute-map)
    (.drawArrays gl gl.TRIANGLES start length)))

(defn run-purefrag-sprog! [gl sprog size uniform-map & [options]]
  (run-sprog! gl
              sprog
              size
              uniform-map
              {"vertPos" (purefrag-vert-pos-boj gl)}
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

(defn run-shaders! [gl sources size uniform-map attribute-map start length
                      & [options]]
  (run-sprog! gl
              (get-autosprog gl sources)
              size
              uniform-map
              attribute-map
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

(defn run-purefrag-shader! [gl source size uniform-map & [options]]
  (run-purefrag-sprog! gl
                       (get-purefrag-autosprog gl source)
                       size
                       uniform-map
                       options))
