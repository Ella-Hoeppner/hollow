(ns sprog.iglu.chunks.postprocessing
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.iglu.core :refer [merge-chunks]]))

(def gaussian-chunk
  '{:functions
    {gaussian {([vec2 float] float)
               ([offset sigma]
                (=vec2 scaledOffset (/ offset sigma))
                (/ (exp (* -0.5 (dot scaledOffset scaledOffset)))
                   (* 6.28 (pow sigma 2))))}}})

(defn star-neighborhood [radius & [skip-factor]]
  (conj (mapcat (comp (fn [r]
                        (list [0 r]
                              [r r]
                              [r 0]
                              [r (- r)]
                              [0 (- r)]
                              [(- r) (- r)]
                              [(- r) 0]
                              [(- r) r]))
                      (partial * (or skip-factor 1)))
                (range 1 (inc radius)))
        [0 0]))

(defn plus-neighborhood [radius & [skip-factor]]
  (conj (mapcat (comp (fn [r]
                        (list [0 r]
                              [r 0]
                              [0 (- r)]
                              [(- r) 0]))
                      (partial * (or skip-factor 1)))
                (range 1 (inc radius)))
        [0 0]))

(defn square-neighborhood [radius & [skip-factor]]
  (mapv (partial mapv (partial * (or skip-factor 1)))
        (for [x (range (- radius) (inc radius))
              y (range (- radius) (inc radius))]
          [x y])))

(defn prescaled-gaussian-sample-expression [neighborhood
                                            value-expression
                                            sigma]
  (let [factors (map (fn [[x y]]
                       (Math/exp
                        (/ (- (+ (* x x) (* y y)))
                           (* 2 sigma sigma))))
                     neighborhood)
        factor-sum (apply + factors)]
    (conj (map (fn [[x y] factor]
                 (list '*
                       (/ factor factor-sum)
                       (postwalk-replace
                        {:x x
                         :y y}
                        value-expression)))
               neighborhood
               factors)
          '+)))

(defn create-gaussian-sample-chunk [texture-name neighborhood]
  (merge-chunks
   gaussian-chunk
   {:functions
    {'gaussianSample
     {'([vec2 vec2 float] vec4)
      (concat
       '([pos offsetFactor sigma]
         (=vec4 sampleSum (vec4 0))
         (=float factorSum 0)
         (=vec2 offset (vec2 0))
         (=float factor 0))
       (mapcat (fn [[x y]]
                 (postwalk-replace
                  {:x x
                   :y y
                   :texture-name texture-name}
                  '((= offset (vec2 :x :y))
                    (= factor (gaussian offset sigma))
                    (+= factorSum factor)
                    (+= sampleSum
                        (* factor
                           (texture :texture-name
                                    (+ pos
                                       (* offsetFactor offset))))))))
               neighborhood)
       '((/ sampleSum factorSum)))}}}))

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

