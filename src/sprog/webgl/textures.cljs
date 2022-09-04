(ns sprog.webgl.textures)

(defn create-texture [gl
                      resolution
                      texture-type
                      & [{:keys [wrap-mode
                                 filter-mode]
                          :or {wrap-mode :repeat
                               filter-mode :linear}}]]
  (let [[width height] (if (number? resolution)
                         [resolution resolution]
                         resolution)
        tex (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (let [internal-format ({:f8 gl.RGBA
                            :u16 gl.RGBA16UI
                            :u32 gl.RGBA32UI}
                           texture-type)
          format ({:f8 gl.RGBA
                   :u16 gl.RGBA_INTEGER
                   :u32 gl.RGBA_INTEGER}
                  texture-type)
          webgl-type ({:f8 gl.UNSIGNED_BYTE
                       :u16 gl.UNSIGNED_SHORT
                       :u32 gl.UNSIGNED_INT}
                      texture-type)]
      (.texImage2D gl
                   gl.TEXTURE_2D
                   0
                   internal-format
                   width
                   height
                   0
                   format
                   webgl-type
                   nil))
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

(defn create-f8-tex [gl resolution & [options]]
  (create-texture gl resolution :f8 options))

(defn create-u16-tex [gl resolution & [options]]
  (create-texture gl resolution :u16 (merge options {:filter-mode :nearest})))

(defn create-u32-tex [gl resolution & [options]]
  (create-texture gl resolution :u32 (merge options {:filter-mode :nearest})))