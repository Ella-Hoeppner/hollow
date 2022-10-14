(ns sprog.webgl.attributes)

(defn ensure-attribute-present! [{:keys [gl program attributes-atom]} 
                                 attrib-name]
  (when (not (@attributes-atom attrib-name))
    (swap! attributes-atom 
           assoc
           attrib-name
           (.getAttribLocation gl
                               program
                               attrib-name))))

(defn set-sprog-attribute! [{:keys [gl attributes-atom] :as sprog}
                            attrib-name
                            value]
  
    (ensure-attribute-present! sprog attrib-name) 
  )

(defn create-attribute [gl posVec v-shader & {:keys [size type normalize
                                                     stride offset attrib-name-str]
                                              :or {size 2
                                                   type (js/FLOAT. gl)
                                                   normalize false
                                                   stride 0
                                                   offset 0
                                                   attrib-name-str "a_position"}}]
  (let [program (.createProgram gl)
        posBuff (.createBuffer gl)
        location (.getAttribLocation gl
                                     program
                                     attrib-name-str)
        vao (.createVertexArray gl)]


    (.bindBuffer gl
                 gl.ARRAY_BUFFER
                 posBuff)
    (.bufferData gl
                 gl.ARRAY_BUFFER
                 (js/Float32Array. posVec)
                 gl.STATIC_DRAW)
    (.bindVertexArray gl vao)
    (.enableVertexAttribArray gl location)
    (.vertexAttribPointer gl
                          location
                          size
                          type
                          normalize
                          stride
                          offset)))