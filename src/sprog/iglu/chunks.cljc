(ns sprog.iglu.chunks)

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
