(ns sprog.webgl.canvas
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
        [width height] (mapv (partial *
                                      (Math/floor
                                       (min pixel-ratio
                                            (/ max-resolution
                                               (max raw-width raw-height)))))
                             [raw-width raw-height])]
    (set! (.-left style) 0)
    (set! (.-top style) 0)
    (set! (.-width style) (str raw-width "px"))
    (set! (.-height style) (str raw-height "px"))
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)))

(defn square-maximize-gl-canvas [gl & {:keys [max-pixel-ratio]}]
  (let [canvas (.-canvas gl)
        raw-width js/window.innerWidth
        raw-height js/window.innerHeight
        raw-size (min raw-width raw-height)
        pixel-ratio (if max-pixel-ratio
                      (min js/window.devicePixelRatio max-pixel-ratio)
                      js/window.devicePixelRatio)
        style canvas.style
        max-resolution (.getParameter gl gl.MAX_TEXTURE_SIZE)
        size (* raw-size
                (Math/floor
                 (min pixel-ratio
                      (/ max-resolution raw-size))))]
    (set! (.-left style) (* (- raw-width raw-size) 0.5))
    (set! (.-top style) (* (- raw-height raw-size) 0.5))
    (set! (.-width style) (str raw-size "px"))
    (set! (.-height style) (str raw-size "px"))
    (set! (.-width canvas) size)
    (set! (.-height canvas) size)))

(defn save-image [gl name]
  (.toBlob gl.canvas
           (fn [blob]
             (let [a (js/document.createElement "a")]
               (js/document.body.appendChild a)
               (let [url (js/window.URL.createObjectURL blob)]
                 (set! a.href url)
                 (set! a.download (str name ".png"))
                 (.click a))
               (js/document.body.removeChild a)))))
