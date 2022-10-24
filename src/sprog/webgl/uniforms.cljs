(ns sprog.webgl.uniforms)

(defn ensure-uniform-present! [gl
                               {:keys [program uniforms-atom]}
                               uniform-name-str]
  (when (not (@uniforms-atom uniform-name-str))
    (swap! uniforms-atom
           assoc
           uniform-name-str
           (.getUniformLocation gl
                                program
                                uniform-name-str))))

(defn set-sprog-uniform-1i! [gl
                             {:keys [uniforms-atom] :as sprog}
                             uniform-name
                             value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform1i gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-2iv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform2iv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-3iv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform3iv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-4iv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform4iv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-1f! [gl
                             {:keys [uniforms-atom] :as sprog}
                             uniform-name
                             value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform1f gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-2fv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform2fv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-3fv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform3fv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-4fv! [gl
                              {:keys [uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniform4fv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-mat2! [gl
                               {:keys [uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniformMatrix2fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-uniform-mat3! [gl
                               {:keys [uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniformMatrix3fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-uniform-mat4! [gl 
                               {:keys [uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! gl sprog uniform-name-str)
    (.uniformMatrix4fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-float-uniform! [gl sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1f!
     (== (count value) 2) set-sprog-uniform-2fv!
     (== (count value) 3) set-sprog-uniform-3fv!
     (== (count value) 4) set-sprog-uniform-4fv!)
   gl sprog uniform-name value))

(defn set-sprog-float-uniforms! [gl sprog name-value-map]
  (doseq [[name value] name-value-map]
    (set-sprog-float-uniform! gl sprog name value)))

(defn set-sprog-int-uniform! [gl sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1i!
     (== (count value) 2) set-sprog-uniform-2iv!
     (== (count value) 3) set-sprog-uniform-3iv!
     (== (count value) 4) set-sprog-uniform-4iv!)
   gl sprog uniform-name value))

(defn set-sprog-int-uniforms! [gl sprog name-value-map]
  (doseq [[name value] name-value-map]
    (set-sprog-int-uniform! gl sprog name value)))

(defn set-sprog-mat-uniform! [gl sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1f!
     (== (count value) 4) set-sprog-uniform-mat2!
     (== (count value) 9) set-sprog-uniform-mat3!
     (== (count value) 16) set-sprog-uniform-mat4!)
   gl sprog uniform-name value))

(defn set-sprog-mat-uniforms! [gl sprog name-mat-map]
  (doseq [[name value] name-mat-map]
    (set-sprog-mat-uniform! gl sprog name value)))

(defn set-sprog-tex-uniforms! [gl sprog name-tex-2d-map name-tex-3d-map]
  (let [name-tex-2d-vec (vec name-tex-2d-map)
        name-tex-3d-vec (vec name-tex-3d-map)]
    (doseq [i (range (count name-tex-2d-vec))]
      (let [[name tex] (name-tex-2d-vec i)]
        (.activeTexture gl (+ gl.TEXTURE0 i))
        (.bindTexture gl gl.TEXTURE_2D tex)
        (set-sprog-uniform-1i! gl sprog name i)))
    (doseq [i (range (count name-tex-3d-vec))]
      (let [[name tex] (name-tex-3d-vec i)
            index (+ i (count name-tex-2d-vec))]
        (.activeTexture gl (+ gl.TEXTURE0 index))
        (.bindTexture gl gl.TEXTURE_3D tex)
        (set-sprog-uniform-1i! gl sprog name index)))))

(defn set-sprog-uniforms! [gl 
                           sprog 
                           {:keys [floats ints textures textures-3d matrices]}]
  (set-sprog-float-uniforms! gl sprog floats)
  (set-sprog-int-uniforms! gl sprog ints)
  (set-sprog-tex-uniforms! gl sprog textures textures-3d)
  (set-sprog-mat-uniforms! gl sprog matrices))
