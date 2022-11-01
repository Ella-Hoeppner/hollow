(ns sprog.iglu.chunks.postprocessing
  (:require [clojure.walk :refer [postwalk-replace]]))

(defn get-simple-gaussian-chunk [& [sample-radius]]
  (let [sample-radius (or sample-radius 16)]
    (postwalk-replace
     {:sample-radius-i (str sample-radius)}
     '{:functions
       {gaussian {([vec2 float] float)
                  ([offset sigma]
                   (=vec2 scaledOffset (/ offset sigma))
                   (/ (exp (* -0.5 (dot scaledOffset scaledOffset)))
                      (* 6.28 (pow sigma 2))))}
        blur {([sampler2D vec2 float] vec4)
              ([tex pos ratio]
               (=int radius :sample-radius-i)
               (=vec4 O (vec4 0))
               ("for (int x = -radius; x <= radius; x++)"
                ("for (int y = -radius; y <= radius; y++)"
                 (=vec2 d (vec2 x y))
                 (+= O (* (gaussian d ratio)
                          (texture tex
                                   (+ pos
                                      (* (/ (vec2 1) size)
                                         d)))))))
               (/ O O.a))}}})))

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
   '{:functions
     {bloom
      {([:sampler-type vec2 float float]  vec4)
      ([tex pos step intensity]
       (=vec4 sum (vec4 0))

       ; 9 samples to blur x axis
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* "4." step))
                                          pos.y)))
                     :divisor)
                  ".05"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* "3." step))
                                          pos.y)))
                     :divisor)
                  ".09"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (- pos.x (* "2." step))
                                          pos.y)))
                     :divisor)
                  ".12"))
       (+= sum (* (/ (vec4 (texture tex (vec2 (- pos.x step)
                                              pos.y)))
                     :divisor)
                  ".15"))
       (+= sum (* (/ (vec4 (texture tex pos)) :divisor) ".16"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x step)
                                          pos.y)))
                     :divisor)
                  ".15"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* "2." step))
                                          pos.y)))
                     :divisor)
                  ".12"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* "3." step))
                                          pos.y)))
                     :divisor)
                  ".09"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 (+ pos.x (* "4." step))
                                          pos.y)))
                     :divisor)
                  ".05"))

       ; 9 more samples blur y axis
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* "4." step)))))
                     :divisor)
                  ".05"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* "3." step)))))
                     :divisor)
                  ".09"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y (* "2." step)))))
                     :divisor)
                  ".12"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (- pos.y step))))
                     :divisor)
                  ".15"))
       (+= sum (* (/ (vec4 (texture tex pos)) :divisor) ".16"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y step))))
                     :divisor)
                  ".15"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* "2." step)))))
                     :divisor)
                  ".12"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* "3." step)))))
                     :divisor)
                  ".09"))
       (+= sum (* (/ (vec4 (texture tex
                                    (vec2 pos.x
                                          (+ pos.y (* "4." step)))))
                     :divisor)
                  ".05"))

       (vec4 (+ (* sum intensity)
                (/ (vec4 (texture tex pos)) :divisor))))}}}))

