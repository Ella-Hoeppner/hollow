(ns hollow.webgl.core
  (:require [hollow.util :as u]
            [hollow.webgl.textures :refer [target-textures!
                                           target-screen!
                                           get-tex-type]]
            [hollow.dom.canvas :refer [create-context
                                       get-context]]))

(defonce hollows-atom (atom nil))

(defn update-hollows! []
  (doseq [hollow (vals @hollows-atom)]
    (when-let [{:keys [state gl update-fn]} hollow]
      (swap! state assoc :gl gl)
      (when update-fn
        (reset! state
                (let [new-state (update-fn @state)]
                  (if (map? new-state)
                    new-state
                    (throw "hollow: update-fn must return a hash-map")))))
      (swap! state assoc :gl gl)))
  (js/requestAnimationFrame update-hollows!))

(defn start-hollow! [init-fn-or-value
                     update-fn
                     & [{:keys [canvas
                                name
                                append-to-body?
                                preserve-drawing-buffer?
                                alpha
                                premultiplied-alpha
                                stencil?]
                         :or {name :default
                              alpha true
                              append-to-body? true
                              premultiplied-alpha true}}]]
  (when-let [old-canvas (js/document.getElementById (str name))]
    (.removeChild old-canvas.parentNode old-canvas))
  (when (nil? @hollows-atom) (update-hollows!))
  (let [gl (if canvas
             (get-context
              canvas
              {"preserveDrawingBuffer" (boolean preserve-drawing-buffer?)
               "alpha" alpha
               "premultipliedAlpha" premultiplied-alpha
               "stencil" (boolean stencil?)})
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
                          (assoc init-state :gl gl)
                          (throw "hollow: init-fn must return a hash-map")))
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
            (reset! state
                    (assoc new-state
                           :gl
                           (:gl (@hollows-atom hollow-name))))
            state))
   new-state)
  ([new-state]
   (set-hollow-state! :default new-state)))

(defn update-hollow-state!
  ([hollow-name update-fn]
   (swap! (get-in @hollows-atom [hollow-name :state])
          (comp #(assoc % :gl (:gl (@hollows-atom hollow-name)))
                update-fn))
   @(get-in @hollows-atom [hollow-name :state]))
  ([update-fn]
   (update-hollow-state! :default update-fn)))

(defn merge-hollow-state!
  ([hollow-name new-state-map]
   (update-hollow-state! hollow-name #(merge % new-state-map)))
  ([new-state-map] (merge-hollow-state! :default new-state-map)))

(defn hollow-context [& hollow-name]
  (:gl (@hollows-atom (or hollow-name :default))))

(defn- clear-float! [gl bit-mask color]
  (let [[r g b a]
        (if (nil? color)
          (repeat 4 0)
          (if (number? color)
            (list color color color 1)
            (case (count color)
              4 color
              3 (concat color (list 1))
              1 (concat (repeat 3 (first color)) (list 1)))))]
    (.clearColor gl r g b a))
  (.clear gl bit-mask))

(defn- clear-uint! [gl color]
  (.clearBufferuiv gl
                   gl.COLOR
                   0
                   (js/Uint32Array.
                    (if (nil? color)
                      (repeat 4 0)
                      (if (number? color)
                        (repeat 4 color)
                        (case (count color)
                          4 color
                          1 (repeat 4 (first color))))))))

(defn clear! [gl & [{:keys [mask
                            color
                            target]
                     :or {mask #{:color :depth :stencil}}}]]
  (let [bit-mask (apply bit-or
                        (map #(or ({:color gl.COLOR_BUFFER_BIT
                                    :depth gl.DEPTH_BUFFER_BIT
                                    :stencil gl.STENCIL_BUFFER_BIT}
                                   %)
                                  %)
                             (if (coll? mask)
                               mask
                               (list mask))))]
    (if target
      (let [tex-type (get-tex-type target)]
        (target-textures! gl target)
        (cond
          (= tex-type :f8)
          (clear-float! gl bit-mask color)

          (= tex-type :u32)
          (clear-uint! gl color)

          :else
          (throw
           (str "hollow: clear! doesn't work for texture type " tex-type))))
      (do (target-screen! gl)
          (clear-float! gl bit-mask color)))))

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
