(ns sprog.webgl.uniforms)

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

(defn set-sprog-uniform-2iv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform2iv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-3iv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform3iv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-4iv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform4iv gl (@uniforms-atom uniform-name-str) value)))

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

(defn set-sprog-uniform-3fv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform3fv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-4fv! [{:keys [gl uniforms-atom] :as sprog}
                              uniform-name
                              value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniform4fv gl (@uniforms-atom uniform-name-str) value)))

(defn set-sprog-uniform-mat2! [{:keys [gl uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniformMatrix2fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-uniform-mat3! [{:keys [gl uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniformMatrix3fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-uniform-mat4! [{:keys [gl uniforms-atom] :as sprog}
                               uniform-name
                               value]
  (let [uniform-name-str (str uniform-name)]
    (ensure-uniform-present! sprog uniform-name-str)
    (.uniformMatrix4fv gl (@uniforms-atom uniform-name-str) false value)))

(defn set-sprog-float-uniform! [sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1f!
     (== (count value) 2) set-sprog-uniform-2fv!
     (== (count value) 3) set-sprog-uniform-3fv!
     (== (count value) 4) set-sprog-uniform-4fv!)
   sprog uniform-name value))

(defn set-sprog-float-uniforms! [sprog name-value-map]
  (doseq [[name value] name-value-map]
    (set-sprog-float-uniform! sprog name value)))

(defn set-sprog-int-uniform! [sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1i!
     (== (count value) 2) set-sprog-uniform-2iv!
     (== (count value) 3) set-sprog-uniform-3iv!
     (== (count value) 4) set-sprog-uniform-4iv!)
   sprog uniform-name value))

(defn set-sprog-int-uniforms! [sprog name-value-map]
  (doseq [[name value] name-value-map]
    (set-sprog-int-uniform! sprog name value)))

(defn set-sprog-mat-uniform! [sprog uniform-name value]
  ((cond
     (number? value) set-sprog-uniform-1f!
     (== (count value) 4) set-sprog-uniform-mat2!
     (== (count value) 9) set-sprog-uniform-mat3!
     (== (count value) 16) set-sprog-uniform-mat4!)
   sprog uniform-name value))

(defn set-sprog-mat-uniforms! [sprog name-mat-map]
  (doseq [[name value] name-mat-map]
    (set-sprog-mat-uniform! sprog name value)))

(defn set-sprog-tex-uniforms! [{:keys [gl] :as sprog} name-tex-map]
  (let [name-tex-vec (vec name-tex-map)]
    (doseq [i (range (count name-tex-vec))]
      (let [[name tex] (name-tex-vec i)]
        (.activeTexture gl (+ gl.TEXTURE0 i))
        (.bindTexture gl gl.TEXTURE_2D tex)
        (set-sprog-uniform-1i! sprog name i)))))

(defn set-sprog-uniforms! [sprog {:keys [floats ints textures matrices]}]
  (set-sprog-float-uniforms! sprog floats)
  (set-sprog-int-uniforms! sprog ints)
  (set-sprog-tex-uniforms! sprog textures)
  (set-sprog-mat-uniforms! sprog matrices))
