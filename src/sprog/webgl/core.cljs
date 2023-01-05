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

(defn start-sprog!
  ([sprog-name init-fn-or-value update-fn]
   (when-let [old-canvas (js/document.getElementById (str sprog-name))]
     (.removeChild old-canvas.parentNode old-canvas))
   (when (nil? @sprogs-atom) (update-sprogs!))
   (let [gl (create-gl-canvas sprog-name true)]
     (swap! sprogs-atom
            assoc
            sprog-name
            {:state (atom
                     (if (fn? init-fn-or-value)
                       (init-fn-or-value gl)
                       init-fn-or-value))
             :gl gl
             :update-fn update-fn})))
  ([init-fn-or-value update-fn]
   (start-sprog! :default init-fn-or-value update-fn)))

(defn sprog-state [& sprog-name]
  (:state (@sprogs-atom (or sprog-name :default))))

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

(defn update-sprog-state! [& args]
  (let [[sprog-name update-fn update-fn-args]
        (if (keyword? (first args))
          [(first args) (second args) (drop 2 args)]
          [:default (first args) (rest args)])]
    (swap! sprogs-atom
           update
           sprog-name
           update
           :state
           (fn [state]
             (apply (partial swap! state update-fn)
                    update-fn-args)
             state))
    @(get-in @sprogs-atom [sprog-name :state])))

(defn sprog-context [& sprog-name]
  (:gl (@sprogs-atom (or sprog-name :default))))
