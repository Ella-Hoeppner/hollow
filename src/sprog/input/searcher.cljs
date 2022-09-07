(ns sprog.input.searcher
  (:require [sprog.input.keyboard :refer [add-left-right-key-callback
                                          add-up-down-key-callback]]
            [sprog.input.mouse :refer [mouse-pos
                                       add-mouse-down-callback]]))

(defonce searcher-values-atom (atom nil))
(defonce searcher-indeces-atom (atom nil))
(defonce mouse-influence-factor-atom (atom nil))

(defonce last-log-str-atom (atom nil))

(defn mouse-influence []
  (let [pos (mouse-pos)
        [x y] pos]
    (if (and (>= x 0)
             (>= y 0)
             (< x 1)
             (< y 1))
      (mapv #(* @mouse-influence-factor-atom (- (* 2 %) 1))
            pos)
      [0 0])))

(defn get-searcher-values []
  (let [[index-1 index-2] @searcher-indeces-atom
        [influence-x influence-y] (mouse-influence)]
    (-> @searcher-values-atom
        (update index-1 (partial + influence-x))
        (update index-2 (partial + influence-y)))))

(defn log-searcher []
  (let [log-str (str "["
                     (apply str
                            (rest (interleave (repeat ", ")
                                              (map #(.toFixed % 3)
                                                   @searcher-values-atom))))
                     "], "
                     (@searcher-indeces-atom 0)
                     " "
                     (@searcher-indeces-atom 1)
                     ", "
                     (.toFixed (first (mouse-influence)) 3)
                     " "
                     (.toFixed (second (mouse-influence)) 3))]
    (when (not= log-str @last-log-str-atom)
      (js/console.log log-str)
      (reset! last-log-str-atom log-str))))

(defn init-searcher! [initial-values & [{:keys [mouse-influence-factor
                                                log?
                                                wasd?]
                                         :or {mouse-influence-factor 1}}]]
  (reset! mouse-influence-factor-atom mouse-influence-factor)
  (reset! searcher-values-atom initial-values)
  (reset! searcher-indeces-atom [0 (if (> (count initial-values) 1) 1 0)])

  (add-mouse-down-callback (fn []
                             (let [[index-1 index-2] @searcher-indeces-atom
                                   [influence-x influence-y] (mouse-influence)]
                               (swap! searcher-values-atom
                                      update
                                      index-1
                                      (partial + influence-x))
                               (swap! searcher-values-atom
                                      update
                                      index-2
                                      (partial + influence-y)))))

  (letfn [(modify-dimension! [index direction]
            (swap! searcher-indeces-atom
                   update
                   index
                   #(mod (+ % direction)
                         (count initial-values))
                   (partial + direction)))]
    (add-left-right-key-callback (partial modify-dimension! 0) wasd?)
    (add-up-down-key-callback (partial modify-dimension! 1) wasd?))

  (letfn [(update-searcher! []
            (when log? (log-searcher))
            (js/requestAnimationFrame update-searcher!))]
    (update-searcher!)))
