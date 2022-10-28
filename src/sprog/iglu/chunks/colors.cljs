(ns sprog.iglu.chunks.colors)

(def hsl-to-rgb-chunk
  '{:signatures {hsl2rgb ([vec3] vec3)}
    :functions {hsl2rgb
                ([color]
                 (=vec3 chroma
                        (clamp (- 2
                                  (abs
                                   (- (mod (+ (* color.x 6)
                                              (vec3 3 1 5))
                                           6)
                                      3)))
                               0
                               1))
                 (mix (mix (vec3 0)
                           (mix (vec3 0.5) chroma color.y)
                           (clamp (* color.z 2) 0 1))
                      (vec3 1)
                      (clamp (- (* color.z 2) 1)
                             0
                             1)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cubic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (* rgb rgb (- 3 (* 2 rgb))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from @Frizzil's comment on https://www.shadertoy.com/view/MsS3Wc
(def quintic-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (* rgb
                           rgb
                           rgb
                           (+ 10 (* rgb (- (* rgb 6) 15)))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cosine-hsv-to-rgb-chunk
  '{:signatures {hsv2rgb ([vec3] vec3)}
    :functions {hsv2rgb
                ([color]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (+ 0.5 (* -0.5 (cos (* rgb 3.14159265359)))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})
