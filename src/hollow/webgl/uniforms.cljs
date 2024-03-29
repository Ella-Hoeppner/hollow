(ns hollow.webgl.uniforms
  (:require [hollow.util :as u]
            [kudzu.tools :refer [clj-name->glsl]]))

(defn ensure-uniform-present! [gl
                               {:keys [program uniform-locations-atom]}
                               uniform-name]
  (when (not (@uniform-locations-atom uniform-name))
    (swap! uniform-locations-atom
           assoc
           uniform-name
           (.getUniformLocation gl
                                program
                                uniform-name))))

(defn set-uniform-int! [gl location value]
  (.uniform1i gl location value))

(defn set-uniform-int-array! [gl location value]
  (.uniform1iv gl location value))

(defn set-uniform-ivec2! [gl location value]
  (.uniform2iv gl location value))

(defn set-uniform-ivec3! [gl location value]
  (.uniform3iv gl location value))

(defn set-uniform-ivec4! [gl location value]
  (.uniform4iv gl location value))

(defn set-uniform-uint! [gl location value]
  (.uniform1ui gl location value))

(defn set-uniform-uint-array! [gl location value]
  (.uniform1uiv gl location value))

(defn set-uniform-uvec2! [gl location value]
  (.uniform2uiv gl location value))

(defn set-uniform-uvec3! [gl location value]
  (.uniform3uiv gl location value))

(defn set-uniform-uvec4! [gl location value]
  (.uniform4uiv gl location value))

(def convert-bool #(if (boolean? %)
                     (if %
                       (long 1)
                       (long 0))
                     %))
(defn set-uniform-bool! [gl location value]
  (set-uniform-int! gl location (convert-bool value)))

(defn set-uniform-bool-array! [gl location value]
  (set-uniform-int-array! gl location (mapv convert-bool value)))

(defn set-uniform-bvec2! [gl location value]
  (set-uniform-ivec2! gl location (mapv convert-bool value)))

(defn set-uniform-bvec3! [gl location value]
  (set-uniform-ivec3! gl location (mapv convert-bool value)))

(defn set-uniform-bvec4! [gl location value]
  (set-uniform-ivec4! gl location (mapv convert-bool value)))

(defn set-uniform-float! [gl location value]
  (.uniform1f gl location value))

(defn set-uniform-float-array! [gl location value]
  (.uniform1fv gl location value))

(defn set-uniform-vec2! [gl location value]
  (.uniform2fv gl location value))

(defn set-uniform-vec3! [gl location value]
  (.uniform3fv gl location value))

(defn set-uniform-vec4! [gl location value]
  (.uniform4fv gl location value))

(defn set-uniform-mat2! [gl location value]
  (.uniformMatrix2fv gl location false value))

(defn set-uniform-mat3! [gl location value]
  (.uniformMatrix3fv gl location false value))

(defn set-uniform-mat4! [gl location value]
  (.uniformMatrix4fv gl location false value))

(defn set-hollow-uniforms! [gl
                            {:keys [uniform-type-map uniform-locations-atom]
                             :as hollow}
                            uniforms]
  (reduce
   (fn [tex-count [uniform-name value]]
     (let [uniform-glsl-name (clj-name->glsl uniform-name)]
       (if-let [uniform-type (uniform-type-map uniform-glsl-name)]
         (do (ensure-uniform-present! gl
                                      hollow
                                      uniform-glsl-name)
             (cond
               (re-matches #"u?sampler[23]D" uniform-type)
               (do (.activeTexture gl (+ gl.TEXTURE0 tex-count))
                   (.bindTexture gl
                                 (if (re-matches #"u?sampler3D" uniform-type)
                                   gl.TEXTURE_3D
                                   gl.TEXTURE_2D)
                                 value)
                   (set-uniform-int! gl
                                     (@uniform-locations-atom
                                      uniform-glsl-name)
                                     tex-count)
                   (inc tex-count))

               (re-matches #"u?sampler[23]D\[[0-9]+\]" uniform-type)
               (let [two-d? (re-matches #"u?sampler2D\[[0-9]+\]" uniform-type)
                     array-size (->> uniform-type
                                     (re-find #"\[[0-9]+\]")
                                     rest
                                     butlast
                                     (apply str)
                                     js/parseInt)]
                 (doseq [[tex index] (map list value (range array-size))]
                   (.activeTexture gl (+ gl.TEXTURE0 (+ tex-count index)))
                   (.bindTexture gl
                                 (if two-d? gl.TEXTURE_2D gl.TEXTURE_3D)
                                 tex))
                 (set-uniform-int-array! gl
                                         (@uniform-locations-atom
                                          uniform-glsl-name)
                                         (vec (range tex-count
                                                     (+ tex-count array-size))))
                 (+ tex-count array-size))

               :else
               (do ((cond
                      (= "float" uniform-type)
                      set-uniform-float!
                      (re-matches #"float\[[0-9]+\]" uniform-type)
                      set-uniform-float-array!
                      (or (= "vec2" uniform-type)
                          (re-matches #"vec2\[[0-9]+\]" uniform-type))
                      set-uniform-vec2!
                      (or (= "vec3" uniform-type)
                          (re-matches #"vec3\[[0-9]+\]" uniform-type))
                      set-uniform-vec3!
                      (or (= "vec4" uniform-type)
                          (re-matches #"vec4\[[0-9]+\]" uniform-type))
                      set-uniform-vec4!

                      (= "int" uniform-type) set-uniform-int!
                      (re-matches #"int\[[0-9]+\]" uniform-type)
                      set-uniform-int-array!
                      (or (= "ivec2" uniform-type)
                          (re-matches #"ivec2\[[0-9]+\]" uniform-type))
                      set-uniform-ivec2!
                      (or (= "ivec3" uniform-type)
                          (re-matches #"ivec3\[[0-9]+\]" uniform-type))
                      set-uniform-ivec3!
                      (or (= "ivec4" uniform-type)
                          (re-matches #"ivec4\[[0-9]+\]" uniform-type))
                      set-uniform-ivec4!

                      (= "uint" uniform-type) set-uniform-uint!
                      (re-matches #"uint\[[0-9]+\]" uniform-type)
                      set-uniform-uint-array!
                      (or (= "uvec2" uniform-type)
                          (re-matches #"uvec2\[[0-9]+\]" uniform-type))
                      set-uniform-uvec2!
                      (or (= "uvec3" uniform-type)
                          (re-matches #"uvec3\[[0-9]+\]" uniform-type))
                      set-uniform-uvec3!
                      (or (= "uvec4" uniform-type)
                          (re-matches #"uvec4\[[0-9]+\]" uniform-type))
                      set-uniform-uvec4!

                      (= "bool" uniform-type) set-uniform-bool!
                      (re-matches #"bool\[[0-9]+\]" uniform-type)
                      set-uniform-bool-array!
                      (or (= "bvec2" uniform-type)
                          (re-matches #"bvec2\[[0-9]+\]" uniform-type))
                      set-uniform-bvec2!
                      (or (= "bvec3" uniform-type)
                          (re-matches #"bvec3\[[0-9]+\]" uniform-type))
                      set-uniform-bvec3!
                      (or (= "bvec4" uniform-type)
                          (re-matches #"bvec4\[[0-9]+\]" uniform-type))
                      set-uniform-bvec4!

                      (or (= "mat2" uniform-type)
                          (re-matches #"mat2\[[0-9]+\]" uniform-type))
                      set-uniform-mat2!
                      (or (= "mat3" uniform-type)
                          (re-matches #"mat3\[[0-9]+\]" uniform-type))
                      set-uniform-mat3!
                      (or (= "mat4" uniform-type)
                          (re-matches #"mat4\[[0-9]+\]" uniform-type))
                      set-uniform-mat4!

                      :else (throw (str "hollow: Unrecognized uniform type \""
                                        uniform-type
                                        "\" for uniform \""
                                        uniform-name
                                        "\"")))
                    gl
                    (@uniform-locations-atom uniform-glsl-name)
                    value)
                   tex-count)))
         (throw
          (str "hollow: No uniform \"" uniform-glsl-name "\" in shader")))))
   0
   uniforms))
