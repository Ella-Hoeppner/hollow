(ns sprog.iglu.chunks.easing
  (:require [sprog.util :as u]
            [sprog.iglu.core :refer [iglu->glsl]]))

; From Inigo Quilez's "Remapping Functions" article
; https://iquilezles.org/articles/functions/

(def cubic-identity 
  '{:functions 
    {cubic-identity
     {([float float float] float)
      ([x m n]
       ("if" (> x m)
             (return x))
       (=float a (- (* 2 n)
                    m))
       (=float b (- (* 2 m)
                    (* 3 n)))
       (=float t (/ x m))
       (+ (* (+ (* a t)
                b)
             t
             t)
          n))}}})

(def sqrt-identity-chunk 
  '{:functions
    {square-identity
     {([float float] float)
      ([x n]
       (sqrt (+ (* x x)
                n)))}}})

(def smooth-unit-identity-chunk 
  '{:functions 
    {smooth-identity 
     {([float] float)
      ([x]
       (* x x (- 2 x)))}}})

(def smoothstep-integral-chunk
  '{:functions 
    {integral-smoothstep
     {([float float] float)
      ([x t]
       ("if" (> x t)
        (return (- x (/ t 2))))
       (* x
          x
          x
          (/ (/ (- 1 (/ (* x 0.5)
                        t))
                t)
             t)))}}})

(def exponential-impulse-chunk
  '{:functions 
    {expo-impulse 
     {([float float] float)
      ([x k]
       (* k x (exp (- 1 (* k x)))))}}})

(def polynomial-impulse-chunk
  '{:functions
    {poly-impulse
     {([float float] float)
      ([k x]
       (/ (* 2
             (sqrt k)
             x)
          (+ 1
             (* k x x))))
      ([float float float] float)
      ([k n x]
       (/ (* (/ n (- n 1))
             (pow (* (- n 1) k)
                  (/ n))
             x)
          (+ 1
             (* k
                (pow x n)))))}}})

(def sustained-impulse-chunk 
  '{:functions 
    {sustain-impulse 
     {([float float float] float)
      ([x f k]
       (=float s (max (- x f) 0))
       (min (/ (* x x)
               (* f f))
            (+ 1
               (* (/ 2 f)
                  s
                  (exp (* "-k"
                          s))))))}}})

(def cubic-pulse-chunk 
  '{:functions 
    {cubic-pulse 
     {([float float float] float)
      ([c w x]
       (= x (abs (- x c)))
       ("if" (> x w)
        (return 0))
       (= x (/ x w))
       (- 1 
          (* x 
             x 
             (- 3
                (* 2 x)))))}}})

(def exponential-step-chunk
  '{:functions
    {exp-step
     {([float float float] float)
      ([x k n]
       (exp (* "-k"
               (pow x n))))}}})

(def gain-chunk 
  '{:functions 
    {gain 
     {([float float] float)
      ([x k]
       (=bool lt? (< x 0.5))
       (=float a (* 0.5
                    (pow (* 2
                            (if lt?
                              x
                              (- 1
                                 x)))
                         k)))
       (if lt?
         a
         (- 1 a)))}}})

(def parabola-chunk 
  '{:functions 
    {parabola 
     {([float float] float)
      ([x k]
       (pow (* 4 x (- 1 x))
            k))}}})

(def power-curve-chunk
  '{:functions 
    {power-curve 
     {([float float float] float)
      ([x a b]
       (=float k (/ (pow (+ a b)
                         (+ a b))
                    (* (pow a a)
                       (pow b b))))
       (* k
          (* (pow x a)
             (pow (- 1 x)
                  b))))}}})

(def sinc-chunk 
  (u/unquotable
   '{:functions
     {sinc
      {([float float] float)
       ([x k]
        (=float a (* ~Math/PI
                     (- (* k x)
                        1)))
        (/ (sin a)
           abs))}}}))

(def quadratic-falloff-chunk 
  '{:functions 
    {falloff 
     {([float float] float)
      ([x m]
       (= x (/ (* (+ x 1)
                  (+ x 1))))
       (= m (/ (* (+ m 1)
                  (+ m 1))))
       (/ (- x m)
          (- 1 m)))}}})

