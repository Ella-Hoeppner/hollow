(ns sprog.webgl.core
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-context
                                      get-context]]))

(defonce sprogs-atom (atom nil))

(defn update-sprogs! []
  (doseq [sprog (vals @sprogs-atom)]
    (when-let [{:keys [state gl update-fn]} sprog]
      (swap! state assoc :gl gl)
      (when update-fn (swap! state update-fn))
      (swap! state assoc :gl gl)))
  (js/requestAnimationFrame update-sprogs!))

(defn start-sprog! [init-fn-or-value
                    update-fn
                    & [{:keys [canvas
                               name
                               append-to-body?
                               preserve-drawing-buffer?
                               stencil?]
                        :or {name :default
                             append-to-body? true}}]]
  (when-let [old-canvas (js/document.getElementById (str name))]
    (.removeChild old-canvas.parentNode old-canvas))
  (when (nil? @sprogs-atom) (update-sprogs!))
  (let [gl (if canvas
             (get-context canvas
                          {"preserveDrawingBuffer"
                           (boolean preserve-drawing-buffer?)
                           "stencil"
                           (boolean stencil?)})
             (create-context
              name
              {:canvas-element canvas
               :append-to-body? append-to-body?
               :preserve-drawing-buffer? preserve-drawing-buffer?
               :stencil? stencil?}))]
    (swap! sprogs-atom
           assoc
           name
           {:state (atom
                    (if (fn? init-fn-or-value)
                      (init-fn-or-value gl)
                      init-fn-or-value))
            :gl gl
            :update-fn update-fn})))

(defn stop-sprog!
  ([sprog-name] (swap! sprogs-atom dissoc sprog-name))
  ([] (stop-sprog! :default)))

(defn sprog-state [& sprog-name]
  @(:state (@sprogs-atom (or sprog-name :default))))

(defn set-sprog-state!
  ([sprog-name new-state]
   (swap! sprogs-atom
          update
          sprog-name
          update
          :state
          (fn [state]
            (reset! state new-state)
            state))
   new-state)
  ([new-state]
   (set-sprog-state! :default new-state)))

(defn update-sprog-state!
  ([sprog-name update-fn]
   (swap! (get-in @sprogs-atom [sprog-name :state])
          update-fn)
   @(get-in @sprogs-atom [sprog-name :state]))
  ([update-fn]
   (update-sprog-state! :default update-fn)))

(defn merge-sprog-state!
  ([sprog-name new-state-map]
   (update-sprog-state! sprog-name #(merge % new-state-map)))
  ([new-state-map] (merge-sprog-state! :default new-state-map)))

(defn sprog-context [& sprog-name]
  (:gl (@sprogs-atom (or sprog-name :default))))

(defn clear! [gl & clear-masks]
  (.clear gl (apply bit-or (map #(or ({:color gl.COLOR_BUFFER_BIT
                                       :depth gl.DEPTH_BUFFER_BIT
                                       :stencil gl.STENCIL_BUFFER_BIT
                                       :all (bit-or gl.COLOR_BUFFER_BIT
                                                    gl.DEPTH_BUFFER_BIT
                                                    gl.STENCIL_BUFFER_BIT)}
                                      %)
                                     %)
                                clear-masks))))

(defn enable! [gl & enable-values]
  (doseq [enable-value enable-values]
    (.enable gl
             (or ({:blend gl.BLEND
                   :cull-face gl.CULL_FACE
                   :depth-test gl.DEPTH_TEST
                   :dither gl.DITHER
                   :polygon-offset-fill gl.POLYGON_OFFSET_FILL
                   :sample-alpha-to-coverage gl.SAMPLE_ALPHA_TO_COVERAGE
                   :sample-coverage gl.SAMPLE_COVERAGE
                   :scissor-test gl.SCISSOR_TEST
                   :stencil-test gl.STENCIL_TEST}
                  enable-value)
                 enable-value))))

(defn disable! [gl & enable-values]
  (doseq [enable-value enable-values]
    (.disable gl
              (or ({:blend gl.BLEND
                    :cull-face gl.CULL_FACE
                    :depth-test gl.DEPTH_TEST
                    :dither gl.DITHER
                    :polygon-offset-fill gl.POLYGON_OFFSET_FILL
                    :sample-alpha-to-coverage gl.SAMPLE_ALPHA_TO_COVERAGE
                    :sample-coverage gl.SAMPLE_COVERAGE
                    :scissor-test gl.SCISSOR_TEST
                    :stencil-test gl.STENCIL_TEST}
                   enable-value)
                  enable-value))))

(defn set-stencil-func! [gl func & [ref mask]]
  (.stencilFunc gl
                (or ({:never gl.NEVER
                      :less gl.LESS
                      :< gl.LESS
                      :equal gl.EQUAL
                      := gl.EQUAL
                      :lequal gl.LEQUAL
                      :<= gl.LEQUAL
                      :greater gl.GREATER
                      :> gl.GREATER
                      :not-equal gl.NOTEQUAL
                      :not= gl.NOTEQUAL
                      :gequal gl.GEQUAL
                      :>= gl.GEQUAL
                      :always gl.ALWAYS} func)
                    func)
                (or ref 0)
                (or mask 0xff)))

(defn set-stencil-op!
  ([gl fail zfail zpass]
   (let [[gl-fail gl-zfail gl-zpass]
         (map (fn [op-param]
                (or ({:keep gl.KEEP
                      :zero gl.ZERO
                      0 gl.ZERO
                      :replace gl.REPLACE
                      :incr gl.INCR
                      :inc gl.INCR
                      :incr-wrap gl.INCR_WRAP
                      :inc-wrap gl.INCR_WRAP
                      :decr gl.DECR
                      :dec gl.DEC
                      :decr-wrap gl.DECR_WARP
                      :invert gl.INVERT}
                     op-param)
                    op-param
                    gl.KEEP))
              [fail zfail zpass])]
     (.stencilOp gl gl-fail gl-zfail gl-zpass)))
  ([gl all]
   (set-stencil-op! gl all all all)))

(defn set-color-mask!
  ([gl r g b a]
   (.colorMask gl (boolean r) (boolean g) (boolean b) (boolean a)))
  ([gl all-channels]
   (set-color-mask! gl all-channels all-channels all-channels all-channels)))
