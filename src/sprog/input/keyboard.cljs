(ns sprog.input.keyboard)

(defonce key-callbacks (atom {}))
(defonce key-toggles (atom {}))
(defonce key-cycles (atom {}))

(defn keydown [event]
  (let [key (.-key event)]
    (swap! key-toggles
           (fn [toggle-map]
             (update toggle-map key not)))
    (swap! key-cycles
           (fn [cycle-map]
             (update cycle-map
                     key
                     (fn [cycle]
                       (when cycle
                         (let [[values index] cycle]
                           [values (mod (inc index) (count values))]))))))
    (doseq [callback (@key-callbacks key)]
      (callback))))

(js/document.addEventListener "keydown" keydown)

(defn key-toggled? [key-str & [default-value]]
  (when (nil? (@key-toggles key-str))
    (swap! key-toggles assoc key-str (boolean default-value)))
  (@key-toggles key-str))

(defn key-cycle [key-str cycle & [default-index]]
  (when (nil? (@key-cycles key-str))
    (swap! key-cycles assoc key-str [cycle (int default-index)]))
  (let [[values index] (@key-cycles key-str)]
    (values index)))

(defn add-key-callback [key-name callback]
  (swap! key-callbacks
         update
         key-name
         #(conj % callback)))

(defn add-num-key-callback [num-callback & [max-num]]
  (doseq [[name num] [["1" 0]
                      ["2" 1]
                      ["3" 2]
                      ["4" 3]
                      ["5" 4]
                      ["6" 5]
                      ["7" 6]
                      ["8" 7]
                      ["9" 8]
                      ["0" 9]
                      ["-" 10]
                      ["=" 11]]]
    (add-key-callback name (partial num-callback num))))

(defn add-shift-num-key-callback [num-callback & [max-num]]
  (doseq [[name num] [["!" 0]
                      ["@" 1]
                      ["#" 2]
                      ["$" 3]
                      ["%" 4]
                      ["^" 5]
                      ["&" 6]
                      ["*" 7]
                      ["(" 8]
                      [")" 9]
                      ["_" 10]
                      ["+" 11]]]
    (add-key-callback name (partial num-callback num))))

(defn add-up-down-key-callback [callback & [wasd?]]
  (add-key-callback "ArrowUp" (partial callback 1))
  (add-key-callback "ArrowDown" (partial callback -1))
  (when wasd?
    (add-key-callback "w" (partial callback 1))
    (add-key-callback "s" (partial callback -1))))

(defn add-left-right-key-callback [callback & [wasd?]]
  (add-key-callback "ArrowLeft" (partial callback -1))
  (add-key-callback "ArrowRight" (partial callback 1))
  (when wasd?
    (add-key-callback "a" (partial callback -1))
    (add-key-callback "d" (partial callback 1))))
