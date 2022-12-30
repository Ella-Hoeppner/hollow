(ns sprog.webgl.core
  (:require [sprog.dom.canvas :refer [create-gl-canvas]]))

(defonce sprogs-atom (atom nil))

(defn update-sprogs! []
  (doseq [sprog (vals @sprogs-atom)]
    (when-let [{:keys [state gl update-fn]} sprog]
      (swap! state (partial update-fn gl))))
  (js/requestAnimationFrame update-sprogs!))

(defn start-sprog!
  ([sprog-name init-fn update-fn]
   (when-let [old-canvas (js/document.getElementById (str sprog-name))]
     (.removeChild old-canvas.parentNode old-canvas))
   (when (nil? @sprogs-atom) (update-sprogs!))
   (let [gl (create-gl-canvas sprog-name true)]
     (swap! sprogs-atom
            update
            sprog-name
            (fn [sprog]
              {:state (atom (if (nil? init-fn)
                              nil
                              (init-fn gl)))
               :gl gl
               :update-fn update-fn}))))
  ([init-fn update-fn]
   (start-sprog! :default init-fn update-fn)))

(defn sprog-state [& sprog-name]
  (:state (@sprogs-atom (or sprog-name :default))))
