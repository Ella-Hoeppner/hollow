(ns hollow.webgl.core
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [create-context
                                       get-context]]))

(defonce hollows-atom (atom nil))

(defn update-hollows! []
  (doseq [hollow (vals @hollows-atom)]
    (when-let [{:keys [state gl update-fn]} hollow]
      (swap! state assoc :gl gl)
      (when update-fn (reset! state
                              (let [new-state (update-fn @state)]
                                (if (map? new-state)
                                  new-state
                                  (throw "update-fn must return a hash-map")))))
      (swap! state assoc :gl gl)))
  (js/requestAnimationFrame update-hollows!))

(defn start-hollow! [init-fn-or-value
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
  (when (nil? @hollows-atom) (update-hollows!))
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
    (swap! hollows-atom
           assoc
           name
           {:state (atom
                    (if (fn? init-fn-or-value)
                      (let [init-state (init-fn-or-value gl)]
                        (if (map? init-state)
                          init-state
                          (throw "init-fn must return a hash-map")))
                      init-fn-or-value))
            :gl gl
            :update-fn update-fn})))

(defn stop-hollow!
  ([hollow-name] (swap! hollows-atom dissoc hollow-name))
  ([] (stop-hollow! :default)))

(defn hollow-state [& hollow-name]
  @(:state (@hollows-atom (or hollow-name :default))))

(defn set-hollow-state!
  ([hollow-name new-state]
   (swap! hollows-atom
          update
          hollow-name
          update
          :state
          (fn [state]
            (reset! state new-state)
            state))
   new-state)
  ([new-state]
   (set-hollow-state! :default new-state)))

(defn update-hollow-state!
  ([hollow-name update-fn]
   (swap! (get-in @hollows-atom [hollow-name :state])
          update-fn)
   @(get-in @hollows-atom [hollow-name :state]))
  ([update-fn]
   (update-hollow-state! :default update-fn)))

(defn merge-hollow-state!
  ([hollow-name new-state-map]
   (update-hollow-state! hollow-name #(merge % new-state-map)))
  ([new-state-map] (merge-hollow-state! :default new-state-map)))

(defn hollow-context [& hollow-name]
  (:gl (@hollows-atom (or hollow-name :default))))

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
