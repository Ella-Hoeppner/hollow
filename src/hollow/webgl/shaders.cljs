(ns hollow.webgl.shaders
  (:require [hollow.util :as u]
            [kudzu.core :refer [kudzu->glsl]]
            [kudzu.chunks.misc :refer [trivial-vert-source]]
            [hollow.webgl.uniforms :refer [set-hollow-uniforms!]]
            [hollow.webgl.textures :refer [target-textures!
                                           target-screen!]]
            [hollow.webgl.attributes :refer [set-hollow-attributes!
                                             set-hollow-attribute!
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

(defn create-hollow [gl vert-source frag-source]
  (let [vert-glsl (if (string? vert-source)
                    vert-source
                    (kudzu->glsl vert-source))
        frag-glsl (if (string? frag-source)
                    frag-source
                    (kudzu->glsl frag-source))
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
                (re-seq #"uniform\s+[A-Za-z0-9\[\]]+\s+[A-Za-z0-9_]+"
                        (str vert-glsl
                             "\n"
                             frag-glsl))))}))

(def purefrag-vert-glsl (kudzu->glsl trivial-vert-source))

(defonce purefrag-vert-pos-bojs-atom (atom {}))

(defn purefrag-vert-pos-boj [gl]
  (when (not (@purefrag-vert-pos-bojs-atom gl))
    (swap! purefrag-vert-pos-bojs-atom
           assoc
           gl
           (create-boj! gl
                        4
                        {:initial-data (js/Float32Array.
                                        (clj->js [-1 -1 -0.99999 1
                                                  -1 3 -0.99999 1
                                                  3 -1 -0.99999 1]))})))
  (@purefrag-vert-pos-bojs-atom gl))

(defn create-purefrag-hollow [gl frag-source]
  (let [hollow (create-hollow gl purefrag-vert-glsl frag-source)]
    (set-hollow-attribute! gl
                           hollow
                           "vertPos"
                           (purefrag-vert-pos-boj gl))
    hollow))

(defn use-hollow! [gl {:keys [program] :as hollow} uniform-map attribute-map]
  (.useProgram gl program)
  (set-hollow-uniforms! gl hollow uniform-map)
  (set-hollow-attributes! gl hollow attribute-map))

(defn run-hollow! [gl hollow size uniform-map attribute-map start length
                   & [{:keys [target indices]}]]
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
    (use-hollow! gl hollow uniform-map attribute-map)
    (if indices
      (do (.bindBuffer gl
                       gl.ELEMENT_ARRAY_BUFFER
                       (:buffer indices))
          (.drawElements gl gl.TRIANGLES length (:type indices) start))
      (.drawArrays gl gl.TRIANGLES start length))))

(defn run-purefrag-hollow! [gl hollow size uniform-map & [options]]
  (run-hollow! gl
               hollow
               size
               uniform-map
               {"vertPos" (purefrag-vert-pos-boj gl)}
               0
               3
               options))

(defonce autohollow-cache-atom (atom {}))

(defn get-autohollow [gl shader-sources]
  (let [autohollow-key [gl shader-sources]]
    (if-let [autohollow (@autohollow-cache-atom autohollow-key)]
      autohollow
      (let [autohollow (apply (partial create-hollow gl) shader-sources)]
        (swap! autohollow-cache-atom assoc autohollow-key autohollow)
        autohollow))))

(defn run-shaders! [gl sources size uniform-map attribute-map start length
                    & [options]]
  (run-hollow! gl
               (get-autohollow gl sources)
               size
               uniform-map
               attribute-map
               start
               length
               options))

(defonce purefrag-autohollow-cache-atom (atom {}))

(defn get-purefrag-autohollow [gl shader-source]
  (let [autohollow-key [gl shader-source]]
    (if-let [autohollow (@purefrag-autohollow-cache-atom autohollow-key)]
      autohollow
      (let [autohollow (create-purefrag-hollow gl shader-source)]
        (swap! purefrag-autohollow-cache-atom assoc autohollow-key autohollow)
        autohollow))))

(defn run-purefrag-shader! [gl source size uniform-map & [options]]
  (run-purefrag-hollow! gl
                        (get-purefrag-autohollow gl source)
                        size
                        uniform-map
                        options))
