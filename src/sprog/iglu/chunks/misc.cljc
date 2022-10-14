(ns sprog.iglu.chunks.misc
  (:require [clojure.walk :refer [postwalk 
                                  postwalk-replace]]))



(def sympow-chunk
  '{:signatures {sympow ([float float] float)}
    :functions
    {sympow
     ([x power]
      (* (sign x)
         (pow (abs x)
              power)))}})

(def smoothstair-chunk
  '{:signatures {smoothstair ([float float float] float)}
    :functions
    {smoothstair
     ([x steps steepness]
      (*= x steps)
      (=float c (- (/ "2.0" (- "1.0" steepness)) "1.0"))
      (=float p (mod x "1.0"))
      (/ (+ (floor x)
            (if (< p "0.5")
              (/ (pow p c)
                 (pow "0.5" (- c "1.0")))
              (- "1.0"
                 (/ (pow (- "1.0" p) c)
                    (pow "0.5" (- c "1.0"))))))
         steps))}})



(defn apply-macros [macro-map expression]
  (postwalk (fn [subexp]
              (if (vector? subexp)
                (let [macro-fn (macro-map (first subexp))]
                  (if macro-fn
                    (apply macro-fn (rest subexp))
                    subexp))
                subexp))
            expression))

(defn offset-shortcut [expression & [rand-fn]]
  (let [rand-fn (or rand-fn rand)]
    (postwalk
     (fn [subexp]
       (if (and (vector? subexp)
                (= (first subexp) :offset))
         (let [[seed-exp
                {:keys [offset-range]
                 :or {offset-range 100}}]
               (rest subexp)]
           (postwalk-replace
            {:x (* offset-range (- (* (rand-fn) 2) 1))
             :y (* offset-range (- (* (rand-fn) 2) 1))
             :seed-exp seed-exp}
            '(+ :seed-exp
                (vec2 :x :y))))
         subexp))
     expression)))



(defn sparse-gaussian-expression [value-fn radius sigma & [skip-factor]]
  (let [coords (conj (mapcat (fn [r]
                               (list [0 r]
                                     [r r]
                                     [r 0]
                                     [r (- r)]
                                     [0 (- r)]
                                     [(- r) (- r)]
                                     [(- r) 0]
                                     [(- r) r]))
                             (range 1 (inc radius) (or skip-factor 1)))
                     [0 0])
        factors (map (fn [[x y]]
                       (Math/exp
                        (/ (- (+ (* x x) (* y y)))
                           (* 2 sigma sigma))))
                     coords)
        factor-sum (apply + factors)]
    (conj (map (fn [[x y] factor]
                 (postwalk-replace
                  {:x (.toFixed x 1)
                   :y (.toFixed y 1)
                   :factor (.toFixed (/ factor factor-sum) 8)
                   :value-fn value-fn}
                  '(* (:value-fn :x :y) :factor)))
               coords
               factors)
          '+)))

(def bilinear-usampler-chunk
  '{:signatures {textureBilinear ([usampler2D vec2] vec4)}
    :functions
    {textureBilinear
     ([tex pos]
      (=vec2 texSize (vec2 (textureSize tex 0)))
      (=vec2 texCoords (- (* pos texSize) "0.5"))
      (=vec2 gridCoords (+ (floor texCoords) "0.5"))
      (=vec2 tweenCoords (fract texCoords))
      #_(vec4 (* "65535.0"
               #_tweenCoords
               (/ gridCoords texSize))
            0
            "65535.0")
      (mix (mix (vec4 (texture tex (/ gridCoords texSize)))
                (vec4 (texture tex (/ (+ gridCoords (vec2 1 0)) texSize)))
                tweenCoords.x)
           (mix (vec4 (texture tex (/ (+ gridCoords (vec2 0 1)) texSize)))
                (vec4 (texture tex (/ (+ gridCoords (vec2 1 1)) texSize)))
                tweenCoords.x)
           tweenCoords.y))}})
