(ns hollow.dom.canvas
  (:require [hollow.util :as u]))

(defn get-context [canvas & options]
  (.getContext canvas
               "webgl2"
               (when options
                 (clj->js options))))

(defn create-context [id & {:keys [append-to-body?
                                   preserve-drawing-buffer?
                                   stencil?]}]
  (let [canvas (js/document.createElement "canvas")
        gl (get-context canvas
                        {"preserveDrawingBuffer"
                         (boolean preserve-drawing-buffer?)
                         "stencil"
                         (boolean stencil?)})]
    (set! (.-id canvas) (str id))
    (when append-to-body?
      (set! (.-position canvas.style) "absolute")
      (.appendChild js/document.body canvas))
    gl))

(defn maximize-canvas [canvas & {:keys [max-pixel-ratio aspect-ratio]}]
  (let [raw-width js/window.innerWidth
        raw-height js/window.innerHeight
        pixel-ratio (if max-pixel-ratio
                      (min js/window.devicePixelRatio max-pixel-ratio)
                      js/window.devicePixelRatio)
        style canvas.style]
    (if aspect-ratio
      (let [height (Math/floor (min (/ raw-width aspect-ratio) raw-height))
            width (Math/floor (* height aspect-ratio))]
        (set! (.-left style) (* (- raw-width width) 0.5))
        (set! (.-top style) (* (- raw-height height) 0.5))
        (set! (.-width style) (str width "px"))
        (set! (.-height style) (str height "px"))
        (set! (.-width canvas) (* width pixel-ratio))
        (set! (.-height canvas) (* height pixel-ratio)))
      (let [[width height] (mapv (partial * pixel-ratio)
                                 [raw-width raw-height])]
        (set! (.-left style) 0)
        (set! (.-top style) 0)
        (set! (.-width style) (str raw-width "px"))
        (set! (.-height style) (str raw-height "px"))
        (set! (.-width canvas) width)
        (set! (.-height canvas) height)))))

(defn maximize-gl-canvas [gl & options]
  (apply (partial maximize-canvas gl.canvas) options))

(defn resize-canvas [canvas pixel-dimensions & {:keys [max-pixel-ratio]}]
  (let [pixel-ratio (if max-pixel-ratio
                      (min js/window.devicePixelRatio max-pixel-ratio)
                      js/window.devicePixelRatio)
        style canvas.style
        [width height] (if (number? pixel-dimensions)
                         [pixel-dimensions pixel-dimensions]
                         pixel-dimensions)
        raw-width js/window.innerWidth
        raw-height js/window.innerHeight
        [style-width style-height] (mapv #(/ % pixel-ratio) [width height])]
    (set! (.-left style) (max 0
                              (* 0.5 (- raw-width style-width))))
    (set! (.-top style) (max 0
                             (* 0.5 (- raw-height style-height))))
    (set! (.-width style) (str (/ width pixel-ratio) "px"))
    (set! (.-height style) (str (/ height pixel-ratio) "px"))
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)))

(defn resize-gl-canvas [gl pixel-dimensions & options]
  (apply (partial resize-canvas gl.canvas pixel-dimensions) options))

(defn canvas-resolution [gl]
  [gl.canvas.width gl.canvas.height])

(defn save-image [canvas name]
  (.toBlob canvas
           (fn [blob]
             (let [a (js/document.createElement "a")]
               (js/document.body.appendChild a)
               (let [url (js/window.URL.createObjectURL blob)]
                 (set! a.href url)
                 (set! a.download (str name ".png"))
                 (.click a))
               (js/document.body.removeChild a)))))

(defn set-page-background-color [color]
  (set! js/document.body.style.backgroundColor
        (apply str "#"
               (map #(let [hex (.toString % 16)]
                       (if (= (count hex) 1)
                         (str "0" hex)
                         hex))
                    color))))
