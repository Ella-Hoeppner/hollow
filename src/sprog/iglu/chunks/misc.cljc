(ns sprog.iglu.chunks.misc
  (:require [clojure.walk :refer [postwalk 
                                  postwalk-replace]]))

(def rescale-chunk
  '{:signatures {rescale ([float float float float float] float)}
    :functions
    {rescale
     ([oldMin oldMax newMin newMax x]
      (+ newMin
         (* (- newMax newMin)
            (/ (- x oldMin)
               (- oldMax oldMin)))))}})

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
      (=float c (- (/ 2 (- 1 steepness)) 1))
      (=float p (mod x 1))
      (/ (+ (floor x)
            (if (< p 0.5)
              (/ (pow p c)
                 (pow 0.5 (- c 1)))
              (- 1
                 (/ (pow (- 1 p) c)
                    (pow 0.5 (- c 1))))))
         steps))}})

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
      (=vec2 texSize (vec2 (textureSize tex i0)))
      (=vec2 texCoords (- (* pos texSize) 0.5))
      (=vec2 gridCoords (+ (floor texCoords) 0.5))
      (=vec2 tweenCoords (fract texCoords))
      (mix (mix (vec4 (texture tex (/ gridCoords texSize)))
                (vec4 (texture tex (/ (+ gridCoords (vec2 1 0)) texSize)))
                tweenCoords.x)
           (mix (vec4 (texture tex (/ (+ gridCoords (vec2 0 1)) texSize)))
                (vec4 (texture tex (/ (+ gridCoords (vec2 1 1)) texSize)))
                tweenCoords.x)
           tweenCoords.y))}})

(defn get-bloom-chunk [texture-type]
  (postwalk-replace
   {:divisor (.toFixed ({:f8 1
                         :u8 (dec (Math/pow 2 8))
                         :u16 (dec (Math/pow 2 16))
                         :u32 (dec (Math/pow 2 32))} texture-type)
                       1)
    :sampler-type (if (= texture-type :f8)
                    'sampler2D
                    'usampler2D)}
   '{:signatures {bloom ([:sampler-type vec2 float float]  vec4)}
     :functions
     {bloom
      ([tex pos step intensity]
       (=vec4 sum (vec4 0))

       ; 9 samples to blur x axis
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* 4 step))
                                          pos.y)))
                     :divisor)
                  0.05))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* 3 step))
                                          pos.y)))
                     :divisor)
                  0.09))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* 2 step))
                                          pos.y)))
                     :divisor)
                  0.12))
       (+= sum (* (/ (vec4 (texture tex (vec2 (- pos.x step)
                                              pos.y)))
                     :divisor)
                  0.15))
       (+= sum (* (/ (vec4 (texture tex pos)) :divisor) ".16"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x step)
                                          pos.y)))
                     :divisor)
                  0.15))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* 2 step))
                                          pos.y)))
                     :divisor)
                  0.12))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* 3 step))
                                          pos.y)))
                     :divisor)
                  0.09))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* 4 step))
                                          pos.y)))
                     :divisor)
                  0.05))

       ; 9 more samples blur y axis
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* 4 step)))))
                     :divisor)
                  0.05))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* 3 step)))))
                     :divisor)
                  0.09))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* 2 step)))))
                     :divisor)
                  0.12))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y step))))
                     :divisor)
                  0.15))
       (+= sum (* (/ (vec4 (texture tex pos)) :divisor) 0.16))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y step))))
                     :divisor)
                  0.15))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* 2 step)))))
                     :divisor)
                  0.12))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* 3 step)))))
                     :divisor)
                  0.09))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* 4 step)))))
                     :divisor)
                  0.05))

       (vec4 (+ (* sum intensity)
                (/ (vec4 (texture tex pos)) :divisor))))}}))

