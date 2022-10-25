(ns sprog.webgl.framebuffers)

(defonce framebuffer-map-atom (atom {}))

(defn target-screen! [gl]
  (.bindFramebuffer gl gl.FRAMEBUFFER nil))

(defn get-framebuffer [gl textures]
  (if-let [framebuffer (@framebuffer-map-atom [gl textures])]
    framebuffer
    (let [framebuffer (.createFramebuffer gl)]
      (.bindFramebuffer gl gl.FRAMEBUFFER framebuffer)
      (doseq [[texture index] (map list textures (range))]
        (str [texture index])
        (if (vector? texture)
          (.framebufferTextureLayer gl
                                    gl.FRAMEBUFFER
                                    (+ gl.COLOR_ATTACHMENT0 index)
                                    (first texture)
                                    0
                                    (second texture))
          (.framebufferTexture2D gl
                                 gl.FRAMEBUFFER
                                 (+ gl.COLOR_ATTACHMENT0 index)
                                 gl.TEXTURE_2D
                                 texture
                                 0)))
      (swap! framebuffer-map-atom assoc [gl textures] framebuffer)
      framebuffer)))

(defn target-textures! [gl & textures]
  (.bindFramebuffer gl gl.FRAMEBUFFER (get-framebuffer gl textures))
  (.drawBuffers gl (map #(+ gl.COLOR_ATTACHMENT0 %)
                        (range (count textures)))))
