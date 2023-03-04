(ns sprog.webgl.core
  (:require [sprog.util :as u]
            [sprog.dom.canvas :refer [create-gl-canvas]]))

(defonce sprogs-atom (atom nil))

(defn update-sprogs! []
  (doseq [sprog (vals @sprogs-atom)]
    (when-let [{:keys [state gl update-fn]} sprog]
      (swap! state assoc :gl gl)
      (swap! state update-fn)
      (swap! state assoc :gl gl)))
  (js/requestAnimationFrame update-sprogs!))

(defn start-sprog! [init-fn-or-value
                    update-fn
                    & [{:keys [name
                               append-to-body?
                               preserve-drawing-buffer?]
                        :or {name :default
                             append-to-body? true}}]]
  (when-let [old-canvas (js/document.getElementById (str name))]
    (.removeChild old-canvas.parentNode old-canvas))
  (when (nil? @sprogs-atom) (update-sprogs!))
  (let [gl (create-gl-canvas
            name
            {:append-to-body? append-to-body?
             :preserve-drawing-buffer? preserve-drawing-buffer?})]
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
