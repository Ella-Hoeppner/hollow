(ns sprog.input.mouse)

(defonce mouse-atom (atom {:pos [0.5 0.5]
                           :down? false
                           :present? false}))

(defonce mouse-down-callbacks-atom (atom []))
(defonce mouse-up-callbacks-atom (atom []))

(defn add-mouse-down-callback [callback]
  (swap! mouse-down-callbacks-atom conj callback))

(defn add-mouse-up-callback [callback]
  (swap! mouse-up-callbacks-atom conj callback))

(defn mouse-pos []
  (:pos @mouse-atom))

(defn mouse-down? []
  (:down? @mouse-atom))

(defn mouse-present? []
  (:present? @mouse-atom))

(set! js/document.onmousemove
      (fn [event]
        (let [x event.clientX
              y event.clientY
              w js/window.innerWidth
              h js/window.innerHeight
              s (min w h)]
          (swap! mouse-atom
                 assoc
                 :pos [(/ (- x (/ (- w s) 2)) s)
                       (/ (- y (/ (- h s) 2)) s)]
                 :present? true))))

(set! js/document.onmousedown
      (fn [_]
        (doseq [callback @mouse-down-callbacks-atom]
          (callback))
        (swap! mouse-atom
               assoc
               :down? true
               :present? true)))

(set! js/document.onmouseup
      (fn [_]
        (doseq [callback @mouse-up-callbacks-atom]
          (callback))
        (swap! mouse-atom
               assoc
               :down? false
               :present? true)))

(set! js/document.onmouseenter
      (fn [_]
        (swap! mouse-atom
               #(-> %
                    (assoc :present? true)))))

(set! js/document.onmouseleave
      (fn [_]
        (swap! mouse-atom
               #(-> %
                    (assoc :pos [0.5 0.5])
                    (assoc :down? false)
                    (assoc :present? false)))))
