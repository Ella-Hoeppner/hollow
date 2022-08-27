(ns input.keyboard)

(defonce key-callbacks (atom {}))

(defn keydown [event]
  (doseq [callback (@key-callbacks (.-key event))]
    (callback)))

(js/document.addEventListener "keydown" keydown)

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
