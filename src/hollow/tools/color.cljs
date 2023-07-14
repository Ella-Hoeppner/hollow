(ns hollow.tools.color
  (:require [hollow.util :as u]))

(defn hsl->rgb [[h s l]]
  (let [chroma (map #(u/clamp (- 2 (Math/abs (- (mod (+ (* h 6) %) 6)
                                                3))))
                    [3 1 5])]
    (map #(u/scale (u/scale 0
                            (u/scale 0 % s)
                            (u/clamp (* l 2)))
                   1
                   (u/clamp (- (* l 2) 1)))
         chroma)))
