(ns hollow.input.mouse)

(defonce mouse-atom (atom {:pos [0.5 0.5]
                           :pixel-pos [-1 -1]
                           :down? false
                           :present? false}))

(defonce mouse-callbacks-atom (atom {}))

(defn add-mouse-down-callback [callback]
  (swap! mouse-callbacks-atom update :down conj callback))

(defn add-mouse-up-callback [callback]
  (swap! mouse-callbacks-atom update :up conj callback))

(defn add-mouse-move-callback [callback]
  (swap! mouse-callbacks-atom update :move conj callback))

(defn add-scroll-x-callback [callback]
  (swap! mouse-callbacks-atom update :scroll-x conj callback))

(defn add-scroll-y-callback [callback]
  (swap! mouse-callbacks-atom update :scroll-y conj callback))

(defn mouse-pos []
  (:pos @mouse-atom))

(defn mouse-element-pixel-pos [element]
  (let [rect (.getBoundingClientRect element)
        [x y] (:pixel-pos @mouse-atom)]
    (when (and (<= rect.left x rect.right)
               (<= rect.top y rect.bottom))
      [(- x rect.left)
       (- y rect.top)])))

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
              s (min w h)
              old-pos (:pos @mouse-atom)
              new-pos (mapv #(- (* 2 %) 1)
                            [(/ (- x (/ (- w s) 2)) s)
                             (- 1 (/ (- y (/ (- h s) 2)) s))])]
          (swap! mouse-atom
                 assoc
                 :pos new-pos
                 :pixel-pos [x y]
                 :present? true)
          (doseq [callback (:move @mouse-callbacks-atom)]
            (callback old-pos)))))

(set! js/document.onmousedown
      (fn [_]
        (doseq [callback (:down @mouse-callbacks-atom)]
          (callback))
        (swap! mouse-atom
               assoc
               :down? true
               :present? true)))

(set! js/document.onmouseup
      (fn [_]
        (doseq [callback (:up @mouse-callbacks-atom)]
          (callback))
        (swap! mouse-atom
               assoc
               :down? false
               :present? true)))

(set! js/document.onmousewheel
      (fn [event]
        (doseq [callback (:scroll-x @mouse-callbacks-atom)]
          (callback event.deltaX))
        (doseq [callback (:scroll-y @mouse-callbacks-atom)]
          (callback event.deltaY))))

(set! js/document.onmouseenter
      (fn [_]
        (swap! mouse-atom
               #(-> %
                    (assoc :present? true)))))

(set! js/document.onmouseleave
      (fn [_]
        (swap! mouse-atom
               #(assoc %
                       :pos [0.5 0.5]
                       :pixel-pos [-1 -1]
                       :down? false
                       :present? false))))
