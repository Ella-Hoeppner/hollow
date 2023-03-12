(ns sprog.webgl.uniforms
  (:require [sprog.util :as u]
            [sprog.iglu.glsl :refer [clj-name->glsl-name]]))

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

(defn set-sprog-uniforms! [gl
                           {:keys [uniform-type-map uniform-locations-atom]
                            :as sprog}
                           uniforms]
  (reduce
   (fn [texture-index [uniform-name value]]
     (let [uniform-glsl-name (str (clj-name->glsl-name uniform-name))]
       (if-let [uniform-type (uniform-type-map uniform-glsl-name)]
         (do (ensure-uniform-present! gl
                                      sprog
                                      uniform-glsl-name)
             (if (#{"sampler2D" "usampler2D" "sampler3D" "usampler3D"}
                  uniform-type)
               (do (.activeTexture gl (+ gl.TEXTURE0 texture-index))
                   (.bindTexture gl
                                 (if (#{"sampler3D" "usampler3D"}
                                      uniform-type)
                                   gl.TEXTURE_3D
                                   gl.TEXTURE_2D)
                                 value)
                   (set-uniform-int! gl
                                     (@uniform-locations-atom uniform-glsl-name)
                                     texture-index)
                   (inc texture-index))
               (do ((cond
                      (= "float" uniform-type)
                      set-uniform-float!
                      (re-find #"float\[[0-9]+\]" uniform-type)
                      set-uniform-float-array!
                      (or (= "vec2" uniform-type)
                          (re-find #"vec2\[[0-9]+\]" uniform-type))
                      set-uniform-vec2!
                      (or (= "vec3" uniform-type)
                          (re-find #"vec3\[[0-9]+\]" uniform-type))
                      set-uniform-vec3!
                      (or (= "vec4" uniform-type)
                          (re-find #"vec4\[[0-9]+\]" uniform-type))
                      set-uniform-vec4!

                      (= "int" uniform-type) set-uniform-int!
                      (re-find #"int\[[0-9]+\]" uniform-type)
                      set-uniform-int-array!
                      (or (= "ivec2" uniform-type)
                          (re-find #"ivec2\[[0-9]+\]" uniform-type))
                      set-uniform-ivec2!
                      (or (= "ivec3" uniform-type)
                          (re-find #"ivec3\[[0-9]+\]" uniform-type))
                      set-uniform-ivec3!
                      (or (= "ivec4" uniform-type)
                          (re-find #"ivec4\[[0-9]+\]" uniform-type))
                      set-uniform-ivec4!

                      (or (= "mat2" uniform-type)
                          (re-find #"mat2\[[0-9]+\]" uniform-type))
                      set-uniform-mat2!
                      (or (= "mat3" uniform-type)
                          (re-find #"mat3\[[0-9]+\]" uniform-type))
                      set-uniform-mat3!
                      (or (= "mat4" uniform-type)
                          (re-find #"mat4\[[0-9]+\]" uniform-type))
                      set-uniform-mat4!

                      :else (throw (str "SPROG: Unrecognized uniform type \""
                                        uniform-type
                                        "\" for uniform \""
                                        uniform-name
                                        "\"")))
                    gl
                    (@uniform-locations-atom uniform-glsl-name)
                    value)
                   texture-index)))
         (throw
          (str "SPROG: No uniform \"" uniform-glsl-name "\" in shader")))))
   0
   uniforms))
