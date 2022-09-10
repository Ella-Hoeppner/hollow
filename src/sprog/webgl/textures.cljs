(ns sprog.webgl.textures)

(defn set-texture-parameters [gl texture filter-mode wrap-mode]
  (.bindTexture gl gl.TEXTURE_2D texture)
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
                    gl-wrap-mode)))

(defn create-texture [gl
                      resolution
                      texture-type
                      & [{:keys [wrap-mode
                                 filter-mode
                                 channels]
                          :or {wrap-mode :repeat
                               filter-mode :linear
                               channels 4}}]]
  (let [[width height] (if (number? resolution)
                         [resolution resolution]
                         resolution)
        tex (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (let [internal-format (({:f8 [gl.R8 gl.RG8 gl.RGB8 gl.RGBA]
                             :u16 [gl.R16UI gl.RG16UI gl.RGB16UI gl.RGBA16UI]
                             :u32 [gl.R32UI gl.RG32UI gl.RGB32UI gl.RGBA32UI]}
                            texture-type)
                           (dec channels))
          format (({:f8 [gl.RED gl.RG gl.RGB gl.RGBA]
                    :u16 [gl.RED_INTEGER 
                          gl.RG_INTEGER
                          gl.RGBA_INTEGER
                          gl.RGBA_INTEGER]
                    :u32 [gl.RED_INTEGER 
                          gl.RG_INTEGER
                          gl.RGBA_INTEGER
                          gl.RGBA_INTEGER]}
                   texture-type)
                  (dec channels))
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
    (set-texture-parameters gl tex filter-mode wrap-mode)
    tex))

(defn create-f8-tex [gl resolution & [options]]
  (create-texture gl resolution :f8 options))

(defn create-u16-tex [gl resolution & [options]]
  (create-texture gl resolution :u16 (merge options {:filter-mode :nearest})))

(defn create-u32-tex [gl resolution & [options]]
  (create-texture gl resolution :u32 (merge options {:filter-mode :nearest})))

(defn html-image-texture [gl img-id & [{:keys [wrap-mode
                                               filter-mode]
                                        :or {wrap-mode :repeat
                                             filter-mode :linear}}]]
  (let [texture (.createTexture gl)
        image (.getElementById js/document img-id)]
    (.bindTexture gl gl.TEXTURE_2D texture)
    (set-texture-parameters gl texture filter-mode wrap-mode)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA
                 gl.RGBA
                 gl.UNSIGNED_BYTE
                 image)
    texture))
