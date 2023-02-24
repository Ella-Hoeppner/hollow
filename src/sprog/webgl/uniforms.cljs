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

(defn set-uniform-ivec2! [gl location value]
  (.uniform2iv gl location value))

(defn set-uniform-ivec3! [gl location value]
  (.uniform3iv gl location value))

(defn set-uniform-ivec4! [gl location value]
  (.uniform4iv gl location value))

(defn set-uniform-float! [gl location value]
  (.uniform1f gl location value))

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
     (let [uniform-glsl-name (clj-name->glsl-name uniform-name)]
       (if-let [uniform-type (uniform-type-map uniform-name)]
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
               (do ((case uniform-type
                      "float" set-uniform-float!
                      "vec2" set-uniform-vec2!
                      "vec3" set-uniform-vec3!
                      "vec4" set-uniform-vec4!
                      "int" set-uniform-int!
                      "ivec2" set-uniform-ivec2!
                      "ivec3" set-uniform-ivec3!
                      "ivec4" set-uniform-ivec4!
                      "mat2" set-uniform-mat2!
                      "mat3" set-uniform-mat3!
                      "mat4" set-uniform-mat4!
                      (throw (str "SPROG: Unrecognized uniform type \""
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
