(ns sprog.webgl.textures)

(defn create-float-tex [gl resolution & [{:keys [wrap-mode
                                                 filter-mode]
                                          :or {wrap-mode :repeat
                                               filter-mode :linear}}]]
  (let [[width height] (if (number? resolution)
                         [resolution resolution]
                         resolution)
        tex (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA
                 width
                 height
                 0
                 gl.RGBA
                 gl.UNSIGNED_BYTE
                 nil)
    (let [gl-filter-mode ({:linear gl.LINEAR
                           :nearest gl.NEAREST}
                          filter-mode)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_MIN_FILTER
                      gl-filter-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_MAG_FILTER
                      gl-filter-mode))
    (let [gl-wrap-mode ({:clamp gl.CLAMP_TO_EDGE
                         :repeat gl.REPEAT}
                        wrap-mode)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      gl-wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      gl-wrap-mode))
    tex))

(defn create-ui16-tex [gl resolution & [{:keys [wrap-mode]
                                         :or {wrap-mode :repeat}}]]
  (let [[width height] (if (number? resolution)
                         [resolution resolution]
                         resolution)
        tex (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA16UI
                 width
                 height
                 0
                 gl.RGBA_INTEGER
                 gl.UNSIGNED_SHORT
                 nil)
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MIN_FILTER
                    gl.NEAREST)
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MAG_FILTER
                    gl.NEAREST)
    (let [gl-wrap-mode ({:clamp gl.CLAMP_TO_EDGE
                         :repeat gl.REPEAT}
                        wrap-mode)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      gl-wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      gl-wrap-mode))
    tex))

(defn create-ui32-tex [gl resolution & [{:keys [wrap-mode]
                                         :or {wrap-mode :repeat}}]]
  (let [[width height] (if (number? resolution)
                         [resolution resolution]
                         resolution)
        tex (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA32UI
                 width
                 height
                 0
                 gl.RGBA_INTEGER
                 gl.UNSIGNED_INT
                 nil)
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MIN_FILTER
                    gl.NEAREST)
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MAG_FILTER
                    gl.NEAREST)
    (let [gl-wrap-mode ({:clamp gl.CLAMP_TO_EDGE
                         :repeat gl.REPEAT}
                        wrap-mode)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      gl-wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      gl-wrap-mode))
    tex))
