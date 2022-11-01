(ns sprog.iglu.chunks.colors)

(def hsl-to-rgb-chunk
  '{:functions {hsl2rgb
                {([vec3] vec3)
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
                              1)))}}})

; derived from https://www.shadertoy.com/view/4dKcWK
(def rgb-to-hsv-chunk
  '{:functions {rgb2hsv
                {([vec3] vec3)
                 ([rgb]
                  (=vec4 p (if (< rgb.g rgb.b)
                             (vec4 rgb.bg -1 0.666666666)
                             (vec4 rgb.gb 0 -0.333333333)))
                  (=vec4 q (if (< rgb.r p.x)
                             (vec4 p.xyw rgb.r)
                             (vec4 rgb.r p.yzx)))
                  (=float c (- q.x (min q.w q.y)))
                  (=float h (abs (+ (/ (- q.w q.y)
                                       (+ "0.0000001"
                                          (* 6 c)))
                                    q.z)))
                  (=vec3 hcv (vec3 h c q.x))
                  (=float s (/ hcv.y (+ hcv.z "0.0000001")))
                  (vec3 hcv.x s hcv.z))}}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def hsv-to-rgb-chunk
  '{:functions {hsv2rgb
                {([vec3] vec3)
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
                                  color.y)))}}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cubic-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
                {([vec3] vec3)
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
                                  color.y)))}}})

; derived from @Frizzil's comment on https://www.shadertoy.com/view/MsS3Wc
(def quintic-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
                {([vec3] vec3)
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
                                  color.y)))}}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cosine-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
                {([vec3] vec3)
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
                                  color.y)))}}})
