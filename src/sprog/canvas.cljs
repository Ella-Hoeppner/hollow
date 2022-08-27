(ns sprog.canvas
  (:require [sprog.util :as u]))

(defn create-gl-canvas []
  (let [canvas (js/document.createElement "canvas")
        gl (.getContext canvas "webgl2")]
    (set! (.-position canvas.style) "absolute")
    (.appendChild js/document.body canvas)
    gl))

(defn maximize-gl-canvas [gl & {:keys [max-pixel-ratio]}]
  (let [canvas (.-canvas gl)
        raw-width js/window.innerWidth
        raw-height js/window.innerHeight
        pixel-ratio (if max-pixel-ratio
                      (min js/window.devicePixelRatio max-pixel-ratio)
                      js/window.devicePixelRatio)
        style canvas.style
        max-resolution (.getParameter gl gl.MAX_TEXTURE_SIZE)
        resolution (mapv (partial *
                                  (Math/floor
                                   (min pixel-ratio
                                        (/ max-resolution
                                           (max raw-width raw-height)))))
                         [raw-width raw-height])
        [width height] resolution]
    (u/log pixel-ratio)
    (set! (.-left style) 0)
    (set! (.-top style) 0)
    (set! (.-width style) (str raw-width "px"))
    (set! (.-height style) (str raw-height "px"))
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)))

(defn save-gl-image [gl name]
  (.toBlob gl.canvas
           (fn [blob]
             (let [a (js/document.createElement "a")]
               (js/document.body.appendChild a)
               (let [url (js/window.URL.createObjectURL blob)]
                 (set! a.href url)
                 (set! a.download (str name ".png"))
                 (.click a))
               (js/document.body.removeChild a)))))
