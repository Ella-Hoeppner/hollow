(ns sprog.iglu.chunks
  (:require [clojure.walk :refer [postwalk 
                                  postwalk-replace]]))

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(def rand-chunk
  '{:signatures {rand ([vec2] float)}
    :functions {rand
                ([p]
                 (=vec3 p3 (fract (* (vec3 p.xyx) "0.1031")))
                 (+= p3 (dot p3 (+ p3.yzx "33.33")))
                 (fract (* (+ p3.x p3.y) p3.z)))}})

(defn random-shortcut [expression & [rand-fn]]
  (let [rand-fn (or rand-fn rand)]
    (postwalk
     (fn [subexp]
       (if (and (vector? subexp)
                (= (first subexp) :rand))
         (postwalk-replace
          {:scale (* (if (> (rand-fn) 0.5) 1 -1)
                     (+ 200 (* (rand-fn) 300)))
           :x (- (* (rand-fn) 100) 50)
           :y (- (* (rand-fn) 100) 50)
           :seed-exp (second subexp)}
          '(rand (+ (* :seed-exp :scale)
                    (vec2 :x :y))))
         subexp))
     expression)))

(def hsl-to-rgb-chunk
  '{:signatures {hsl2rgb ([vec3] vec3)}
    :functions {hsl2rgb
                ([color]
                 (=vec3 chroma
                        (clamp (- "2.0"
                                  (abs
                                   (- (mod (+ (* color.x "6.0")
                                              (vec3 "3.0"
                                                    "1.0"
                                                    "5.0"))
                                           "6.0")
                                      "3.0")))
                               "0.0"
                               "1.0"))
                 (mix (mix (vec3 0)
                           (mix (vec3 "0.5") chroma color.y)
                           (clamp (* color.z "2.0") "0.0" "1.0"))
                      (vec3 1)
                      (clamp (- (* color.z "2.0") "1.0")
                             "0.0"
                             "1.0")))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cubic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (* rgb rgb (- "3.0" (* "2.0" rgb))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from @Frizzil's comment on https://www.shadertoy.com/view/MsS3Wc
(def quintic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (* rgb
                           rgb
                           rgb
                           (+ "10.0" (* rgb (- (* rgb "6.0") "15.0")))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cosine-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x "6.0")
                                                      (vec3 0 4 2))
                                                   "6.0")
                                              "3.0"))
                                      "1.0")
                                   "0.0"
                                   "1.0"))
                 (= rgb (+ "0.5" (* "-0.5" (cos (* rgb "3.14159265359")))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

(def particle-vert-source
  '{:precision {float highp
                int highp
                usampler2D highp}
    :outputs {particlePos vec2}
    :uniforms {particleTex usampler2D
               radius float}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=int agentIndex (/ gl_VertexID 6))
      (=int corner "gl_VertexID % 6")

      (=ivec2 texSize (textureSize particleTex 0))

      (=vec2 texPos
             (/ (+ "0.5" (vec2 (% agentIndex texSize.x)
                               (/ agentIndex texSize.x)))
                (vec2 texSize)))

      (=uvec4 particleColor (texture particleTex texPos))
      (= particlePos (/ (vec2 particleColor.xy) "65535.0"))

      (= gl_Position
         (vec4 (- (* (+ particlePos
                        (* radius
                           (- (* "2.0"
                                 (if (|| (== corner 0)
                                         (== corner 3))
                                   (vec2 0 1)
                                   (if (|| (== corner 1)
                                           (== corner 4))
                                     (vec2 1 0)
                                     (if (== corner 2)
                                       (vec2 0 0)
                                       (vec2 1 1)))))
                              "1.0")))
                     "2.0")
                  "1.0")
               0
               1)))}})

(def particle-frag-source
  '{:precision {float highp
                int highp}
    :uniforms {radius float
               size float}
    :inputs {particlePos vec2}
    :outputs {fragColor vec4}
    :signatures {main ([] void)}
    :functions
    {main
     ([]
      (=vec2 pos (/ gl_FragCoord.xy size))
      (=float dist (distance pos particlePos))
      ("if" (> dist radius)
            "discard")
      (= fragColor (vec4 1 0 0 1)))}})

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
