(ns sprog.webgl.framebuffers)

(defn target-screen! [gl]
  (.bindFramebuffer gl gl.FRAMEBUFFER nil))

(defn target-textures! [gl framebuffer & textures]
  (.bindFramebuffer gl gl.FRAMEBUFFER framebuffer)
  (doseq [[texture index] (map list textures (range))]
    (.framebufferTexture2D gl
                           gl.FRAMEBUFFER
                           (+ gl.COLOR_ATTACHMENT0 index)
                           gl.TEXTURE_2D
                           texture
                           0)))
