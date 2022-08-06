(ns sprog.textures)

(defn create-float-tex [gl resolution & [clamp?]]
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
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MIN_FILTER
                    gl.LINEAR)
    (.texParameteri gl
                    gl.TEXTURE_2D
                    gl.TEXTURE_MAG_FILTER
                    gl.LINEAR)
    (let [wrap-mode (if clamp?
                      gl.CLAMP_TO_EDGE
                      gl.REPEAT)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      wrap-mode))
    tex))

(defn create-ui16-tex [gl resolution & [clamp?]]
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
    (let [wrap-mode (if clamp?
                      gl.CLAMP_TO_EDGE
                      gl.REPEAT)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      wrap-mode))
    tex))

(defn create-ui32-tex [gl resolution & [clamp?]]
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
    (let [wrap-mode (if clamp?
                      gl.CLAMP_TO_EDGE
                      gl.REPEAT)]
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_S
                      wrap-mode)
      (.texParameteri gl
                      gl.TEXTURE_2D
                      gl.TEXTURE_WRAP_T
                      wrap-mode))
    tex))
