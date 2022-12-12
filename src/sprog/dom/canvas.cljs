(ns sprog.dom.canvas
  (:require [sprog.util :as u]))

(defn create-gl-canvas [& [append-to-body?]]
  (let [canvas (js/document.createElement "canvas")
        gl (.getContext canvas "webgl2")]
    (when append-to-body?
      (set! (.-position canvas.style) "absolute")
      (.appendChild js/document.body canvas))
    gl))

(defn maximize-canvas [canvas & {:keys [max-pixel-ratio square?]}]
  (let [raw-width js/window.innerWidth
        raw-height js/window.innerHeight
        pixel-ratio (if max-pixel-ratio
                      (min js/window.devicePixelRatio max-pixel-ratio)
                      js/window.devicePixelRatio)
        style canvas.style]
    (if square?
      (let [raw-size (min raw-width raw-height)
            pixel-ratio (if max-pixel-ratio
                          (min js/window.devicePixelRatio max-pixel-ratio)
                          js/window.devicePixelRatio)
            size (* raw-size pixel-ratio)]
        (set! (.-left style) (* (- raw-width raw-size) 0.5))
        (set! (.-top style) (* (- raw-height raw-size) 0.5))
        (set! (.-width style) (str raw-size "px"))
        (set! (.-height style) (str raw-size "px"))
        (set! (.-width canvas) size)
        (set! (.-height canvas) size))
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
