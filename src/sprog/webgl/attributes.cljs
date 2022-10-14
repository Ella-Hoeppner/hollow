(ns sprog.webgl.attributes)

(defn set-buffer-data! [gl buffer data]
  (.bindBuffer gl
               gl.ARRAY_BUFFER
               buffer)
  (.bufferData gl
               gl.ARRAY_BUFFER
               data
               gl.STATIC_DRAW))

(defn ensure-attribute-present! [{:keys [gl program attributes-atom]} 
                                 attrib-name-str]
  (when (not (@attributes-atom attrib-name-str))
    (swap! attributes-atom 
           assoc
           attrib-name-str
           (.getAttribLocation gl
                               program
                               attrib-name-str))))

(defn set-sprog-attribute! [{:keys [gl attributes-atom] :as sprog}
                            attrib-name
                            buffer
                            num-components
                            & [{:keys [type
                                       normalize
                                       stride
                                       offset]
                                :or {type gl.FLOAT
                                     normalize false
                                     stride 0
                                     offset 0}}]]
  (let [attrib-name-str (str attrib-name)]
    (ensure-attribute-present! sprog attrib-name-str)
    (let [location (@attributes-atom attrib-name)]
      (.bindBuffer gl gl.ARRAY_BUFFER buffer)
      (.enableVertexAttribArray gl location)
      (.vertexAttribPointer gl
                            location
                            num-components
                            type
                            normalize
                            stride
                            offset))))
