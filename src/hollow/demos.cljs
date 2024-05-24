(ns hollow.demos
  (:require [hollow.demos.arrays :as arrays]
            [hollow.demos.basic :as basic]
            [hollow.demos.bilinear :as bilinear]
            [hollow.demos.bloom :as bloom]
            [hollow.demos.blur :as blur]
            [hollow.demos.clear :as clear]
            [hollow.demos.draw-elements :as draw-elements]
            [hollow.demos.fbm :as fbm]
            [hollow.demos.gabor :as gabor]
            [hollow.demos.hsv :as hsv]
            [hollow.demos.macros :as macros]
            [hollow.demos.midi :as midi]
            [hollow.demos.mix-oklab :as mix-oklab]
            [hollow.demos.multi-texture-output :as multi-texture-output]
            [hollow.demos.nonconvex-polygon :as nonconvex-polygon]
            [hollow.demos.okhsl :as okhsl]
            [hollow.demos.pcg-hash :as pcg-hash]
            [hollow.demos.physarum :as physarum]
            [hollow.demos.pixel-sort :as pixel-sort]
            [hollow.demos.raymarch :as raymarch]
            [hollow.demos.simplex :as simplex]
            [hollow.demos.stencil :as stencil]
            [hollow.demos.sub-data :as sub-data]
            [hollow.demos.texture-3d :as texture-3d]
            [hollow.demos.texture-arrays :as texture-arrays]
            [hollow.demos.texture-depth :as texture-depth]
            [hollow.demos.tilable-simplex :as tilable-simplex]
            [hollow.demos.vertex :as vertex]
            [hollow.demos.video :as video]
            [hollow.demos.voronoise :as voronoise]
            [hollow.demos.webcam :as webcam]
            [goog.dom :as gdom]
            [goog.events :as events]))

;; Define a map of demo namespaces and their init functions
(def demos
  {:arrays arrays/init
   :basic basic/init
   :bilinear bilinear/init
   :bloom bloom/init
   :blur blur/init
   :clear clear/init
   :draw-elements draw-elements/init
   :fbm fbm/init
   :gabor gabor/init
   :hsv hsv/init
   :macros macros/init
   :midi midi/init
   :mix-oklab mix-oklab/init
   :multi-texture-output multi-texture-output/init
   :nonconvex-polygon nonconvex-polygon/init
   :okhsl okhsl/init
   :pcg-hash pcg-hash/init
   :physarum physarum/init
   :pixel-sort pixel-sort/init
   :raymarch raymarch/init
   :simplex simplex/init
   :stencil stencil/init
   :sub-data sub-data/init
   :texture-3d texture-3d/init
   :texture-arrays texture-arrays/init
   :texture-depth texture-depth/init
   :tilable-simplex tilable-simplex/init
   :vertex vertex/init
   :video video/init
   :voronoise voronoise/init
   :webcam webcam/init})

;; Function to create a dropdown menu
(defn create-dropdown []
  (let [select (gdom/createElement "select")
        sorted-keys (sort (keys demos))]
    ;; Add a default option
    (let [default-option (gdom/createElement "option")]
      (set! (.-value default-option) "")
      (set! (.-textContent default-option) "Select a demo")
      (gdom/appendChild select default-option))
    ;; Add sorted demo options
    (doseq [key sorted-keys]
      (let [option (gdom/createElement "option")]
        (set! (.-value option) (name key))
        (set! (.-textContent option) (name key))
        (gdom/appendChild select option)))
    select))

;; Event handler for demo selection
(defn on-demo-select [event]
  (let [selected-demo (keyword (.. event -target -value))]
    (if (not= selected-demo "")
      (do
        (js/console.log "Selected demo:" (name selected-demo))
        (if-let [init-fn (get demos selected-demo)]
          (do
            (js/console.log "Initializing demo:" (name selected-demo))
            (init-fn))
          (js/console.log "No init function found for:" (name selected-demo))))
      (js/console.log "No demo selected"))))

;; Main init function
(defn init []
  (let [app-element (gdom/getElement "app")]
    (assert app-element "The element with id 'app' was not found in the DOM.")
    (let [select (create-dropdown)]
      (gdom/appendChild app-element select)
      (events/listen select "change" on-demo-select)
      ;; Ensure the dropdown starts with the default option
      (set! (.-value select) "")
      ;; Manually create and dispatch a synthetic event to simulate selection change
      (let [event (js/Event. "change" #js {:bubbles true})]
        (.dispatchEvent select event)))))
