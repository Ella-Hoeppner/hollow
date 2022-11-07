(ns sprog.webgl.textures)

(defonce framebuffer-map-atom (atom {}))

(defn get-framebuffer [gl textures]
  (if-let [framebuffer (@framebuffer-map-atom [gl textures])]
    framebuffer
    (let [framebuffer (.createFramebuffer gl)]
      (.bindFramebuffer gl gl.FRAMEBUFFER framebuffer)
      (doseq [[texture index] (map list textures (range))]
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

(defn target-screen! [gl]
  (.bindFramebuffer gl gl.FRAMEBUFFER nil))

(defn target-textures! [gl & textures]
  (.bindFramebuffer gl gl.FRAMEBUFFER (get-framebuffer gl textures))
  (.drawBuffers gl (map #(+ gl.COLOR_ATTACHMENT0 %)
                        (range (count textures)))))

(defn set-tex-parameters [gl texture filter-mode wrap-mode & [three-d?]]
  (let [texture-target (if three-d? gl.TEXTURE_3D gl.TEXTURE_2D)]
    (.bindTexture gl texture-target texture)
    (let [gl-filter-mode ({:linear gl.LINEAR
                           :nearest gl.NEAREST}
                          filter-mode)]
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_MIN_FILTER
                      gl-filter-mode)
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_MAG_FILTER
                      gl-filter-mode))
    (let [wrap-mode->gl-enum (fn [mode]
                               (case mode
                                 :clamp gl.CLAMP_TO_EDGE
                                 :repeat gl.REPEAT
                                 :mirror gl.MIRRORED_REPEAT
                                 mode))
          [gl-wrap-s gl-wrap-t gl-wrap-r]
          (if (coll? wrap-mode)
            (map wrap-mode->gl-enum wrap-mode)
            (repeat (wrap-mode->gl-enum wrap-mode)))]
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_WRAP_S
                      gl-wrap-s)
      (.texParameteri gl
                      texture-target
                      gl.TEXTURE_WRAP_T
                      gl-wrap-t)
      (when three-d?
        (.texParameteri gl
                        texture-target
                        gl.TEXTURE_WRAP_R
                        gl-wrap-r)))))

(defn create-tex [gl
                  texture-type
                  resolution
                  & [{:keys [wrap-mode
                             filter-mode
                             channels
                             data]
                      :or {wrap-mode :repeat
                           channels 4}
                      three-d? :3d}]]
  (let [texture-target (if three-d? gl.TEXTURE_3D gl.TEXTURE_2D)
        tex (.createTexture gl texture-target)]
    (.bindTexture gl texture-target tex)
    (let [internal-format (({:f8 [gl.R8 gl.RG8 gl.RGB8 gl.RGBA]
                             :u16 [gl.R16UI gl.RG16UI gl.RGB16UI gl.RGBA16UI]
                             :u32 [gl.R32UI gl.RG32UI gl.RGB32UI gl.RGBA32UI]}
                            texture-type)
                           (dec channels))
          format (({:f8 [gl.RED gl.RG gl.RGB gl.RGBA]
                    :u16 [gl.RED_INTEGER
                          gl.RG_INTEGER
                          gl.RGB_INTEGER
                          gl.RGBA_INTEGER]
                    :u32 [gl.RED_INTEGER
                          gl.RG_INTEGER
                          gl.RGB_INTEGER
                          gl.RGBA_INTEGER]}
                   texture-type)
                  (dec channels))
          webgl-type ({:f8 gl.UNSIGNED_BYTE
                       :u16 gl.UNSIGNED_SHORT
                       :u32 gl.UNSIGNED_INT}
                      texture-type)]
      (if three-d?
        (let [[width height depth] (if (number? resolution)
                                     [resolution resolution resolution]
                                     resolution)]
          (.texImage3D gl
                       gl.TEXTURE_3D
                       0
                       internal-format
                       width
                       height
                       depth
                       0
                       format
                       webgl-type
                       data))
        (let [[width height] (if (number? resolution)
                               [resolution resolution]
                               resolution)]
          (.texImage2D gl
                       gl.TEXTURE_2D
                       0
                       internal-format
                       width
                       height
                       0
                       format
                       webgl-type
                       data))))
    (set-tex-parameters gl
                        tex
                        (or filter-mode
                            (if (= texture-type :f8)
                              :linear
                              :nearest))
                        wrap-mode
                        three-d?)
    tex))

(defn delete-tex [gl & textures]
  (doseq [tex (flatten textures)]
    (.deleteTexture gl tex)))

(defn tex-data-array [gl texture texture-type size]
  (target-textures! gl texture)
  (let [[x y width height]
        (cond
          (number? size) [0 0 size size]
          (= (count size) 2) (into [0 0] size)
          (= (count size) 4) size)
        array (case texture-type
                :f8 (js/Uint8Array. (* width height 4))
                :u16 (js/Uint16Array. (* width height 4))
                :u32 (js/Uint32Array. (* width height 4)))]
    (case texture-type
      :f8
      (.readPixels gl x y width height gl.RGBA gl.UNSIGNED_BYTE array)
      :u16
      (.readPixels gl x y width height gl.RGBA_INTEGER gl.UNSIGNED_SHORT array)
      :u32
      (.readPixels gl x y width height gl.RGBA_INTEGER gl.UNSIGNED_INT array))
    array))

(defn copy-html-image-data! [gl tex element-or-id]
  (let [element (if (string? element-or-id)
                  (.getElementById js/document element-or-id)
                  element-or-id)]
    (.bindTexture gl gl.TEXTURE_2D tex)
    (.texImage2D gl
                 gl.TEXTURE_2D
                 0
                 gl.RGBA
                 gl.RGBA
                 gl.UNSIGNED_BYTE
                 element)))

(defn html-image-tex [gl element-or-id & [{:keys [wrap-mode
                                                  filter-mode]
                                           :or {wrap-mode :repeat
                                                filter-mode :linear}}]]
  (let [texture (.createTexture gl)]
    (.bindTexture gl gl.TEXTURE_2D texture)
    (set-tex-parameters gl texture filter-mode wrap-mode)
    (copy-html-image-data! gl texture element-or-id)
    texture))

(defn create-webcam-video-element [callback & [{:keys [width 
                                                       height 
                                                       brightness]
                                                :or {width 1024
                                                     height 1024
                                                     brightness 2}}]]
  (let [media-constraints (clj->js {:audio false
                                    :video {:width width
                                            :height height
                                            :brightness {:ideal brightness}}})
        video (js/document.createElement "video")]
    (.then (js/navigator.mediaDevices.getUserMedia media-constraints)
           (fn [media-stream]
             (set! video.srcObject media-stream)
             (.setAttribute video "playsinline" true)
             (set! video.onloadedmetadata
                   (fn [e]
                     (.play video)
                     (callback video)))))))
