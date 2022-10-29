(ns sprog.iglu.chunks.noise
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.iglu.core :refer [merge-chunks]]))

(def rand-chunk
  '{:functions {rand
                {([vec2] float)
                 ([p]
                  (=vec3 p3 (fract (* (vec3 p.xyx) 0.1031)))
                  (+= p3 (dot p3 (+ p3.yzx 33.33)))
                  (fract (* (+ p3.x p3.y) p3.z)))}}})

; based on https://thebookofshaders.com/edit.php#11/2d-snoise-clear.frag
(def simplex-2d-chunk
  (postwalk-replace
   {:c (conj (list (/ (- 3 (Math/sqrt 3)) 6)
                   (/ (- (Math/sqrt 3) 1) 2)
                   (- (/ (- 3 (Math/sqrt 3)) 3) 1)
                   (/ 1 41))
             'vec4)}
   '{:functions
     {mod289 {([vec2] vec2)
              ([x] (- x (* (floor (/ x 289)) 289)))
              ([vec3] vec3)
              ([x] (- x (* (floor (/ x 289)) 289)))}
      permute {([vec3] vec3)
               ([x] (mod289 (* x (+ 1 (* x 34)))))}
      snoise2D
      {([vec2] float)
       ([v]
        (+= v (vec2 12.5 -3.6))
        (=vec4 C :c)
        (=vec2 i (floor (+ v (dot v C.yy))))
        (=vec2 x0 (- (+ v (dot i C.xx))
                     i))

        (=vec2 c1 (if (> x0.x x0.y) (vec2 1 0) (vec2 0 1)))
        (=vec2 x1 (- (+ x0.xy C.xx) c1))
        (=vec2 x2 (+ x0.xy C.zz))

        (= i (mod289 i))

        (=vec3 p (permute
                  (+ (permute (+ i.y (vec3 0 c1.y 1)))
                     i.x
                     (vec3 0 c1.x 1))))
        (=vec3 m (max (vec3 0)
                      (- 0.5
                         (vec3 (dot x0 x0)
                               (dot x1 x1)
                               (dot x2 x2)))))

        (= m (* m m))
        (= m (* m m))

        (=vec3 x (- (* 2 (fract (* p C.www))) 1))
        (=vec3 h (- (abs x) 0.5))
        (=vec3 ox (floor (+ x 0.5)))
        (=vec3 a0 (- x ox))

        (*= m (- 1.79284291400159
                 (* 0.85373472095314
                    (+ (* a0 a0)
                       (* h h)))))

        (=vec3 g (vec3 (+ (* a0.x x0.x) (* h.x x0.y))
                       (+ (* a0.yz (vec2 x1.x x2.x))
                          (* h.yz (vec2 x1.y x2.y)))))
        (* 130 (dot m g)))}}}))

; based on https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
(def simplex-3d-chunk
  {:functions
   '{permute {([vec4] vec4)
              ([x] (mod (* x (+ 1 (* 34 x))) 289))}
     taylorInvSqrt {([vec4] vec4)
                    ([r] (- 1.79284291400159
                            (* r 0.85373472095314)))}
     snoise3D
     {([vec3] float)
      ([v]
       (=vec2 C (vec2 (/ 1 6) (/ 1 3)))
       (=vec4 D (vec4 0 0.5 1 2))

      ; first corner
       (=vec3 i (floor (+ v (dot v C.yyy))))
       (=vec3 x0 (+ (- v i)
                    (dot i C.xxx)))

      ; other corners
       (=vec3 g (step x0.yzx x0.xyz))
       (=vec3 l (- 1 g))
       (=vec3 c1 (min g.xyz l.zxy))
       (=vec3 c2 (max g.xyz l.zxy))

       (=vec3 x1 (+ (- x0 c1) C.xxx))
       (=vec3 x2 (+ (- x0 c2) (* 2 C.xxx)))
       (=vec3 x3 (+ (- x0 1) (* 3 C.xxx)))

      ; permutations
       (= i (mod i 289))
       (=vec4 p (permute (+ (permute (+ (permute (+ i.z (vec4 0 c1.z c2.z 1)))
                                        i.y
                                        (vec4 0 c1.y c2.y 1)))
                            i.x
                            (vec4 0 c1.x c2.x 1))))

      ; gradients
       (=vec3 ns (- (* D.wyz (/ 1 7)) D.xzx))

       (=vec4 j (- p (* 49 (floor (* p ns.z ns.z)))))

       (=vec4 x_ (floor (* j ns.z)))
       (=vec4 y_ (floor (- j (* 7 x_))))

       (=vec4 x (+ ns.yyyy (* ns.x x_)))
       (=vec4 y (+ ns.yyyy (* ns.x y_)))
       (=vec4 h (- 1 (+ (abs x) (abs y))))

       (=vec4 b0 (vec4 x.xy y.xy))
       (=vec4 b1 (vec4 x.zw y.zw))

       (=vec4 s0 (+ 1 (* 2 (floor b0))))
       (=vec4 s1 (+ 1 (* 2 (floor b1))))
       (=vec4 sh (- 0 (step h (vec4 0))))

       (=vec4 a0 (+ b0.xzyw (* s0.xzyw sh.xxyy)))
       (=vec4 a1 (+ b1.xzyw (* s1.xzyw sh.zzww)))

       (=vec3 p0 (vec3 a0.xy h.x))
       (=vec3 p1 (vec3 a0.zw h.y))
       (=vec3 p2 (vec3 a1.xy h.z))
       (=vec3 p3 (vec3 a1.zw h.w))

      ; normalize gradients
       (=vec4 norm (taylorInvSqrt (vec4 (dot p0 p0)
                                        (dot p1 p1)
                                        (dot p2 p2)
                                        (dot p3 p3))))

       (*= p0 norm.x)
       (*= p1 norm.y)
       (*= p2 norm.z)
       (*= p3 norm.w)

      ; mix final noise value
       (=vec4 m (max (- 0.6
                        (vec4 (dot x0 x0)
                              (dot x1 x1)
                              (dot x2 x2)
                              (dot x3 x3)))
                     0))
       (*= m m)
       (* 42 (dot (* m m)
                  (vec4 (dot p0 x0)
                        (dot p1 x1)
                        (dot p2 x2)
                        (dot p3 x3)))))}}})

; based on https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
(def simplex-4d-chunk
  '{:functions
    {permute {([float] float)
              ([x] (floor (mod (* x (+ 1 (* x 34))) 289)))
              ([vec4] vec4)
              ([x] (mod (* x (+ 1 (* x 34))) 289))}
     taylorInvSqrt {([float] float)
                    ([r] (- 1.79284291400159 (* 0.85373472095314 r)))
                    ([vec4] vec4)
                    ([r] (- 1.79284291400159 (* 0.85373472095314 r)))}
     grad4
     {([float vec4] vec4)
      ([j ip]
       (=vec4 p (vec4 (- (* (floor (* (fract (* j ip.xyz)) 7))
                            ip.z)
                         1)
                      0))
       (= p.w (- 1.5
                 (+ (abs p.x)
                    (abs p.y)
                    (abs p.z))))
       (=vec4 s (vec4 (lessThan p (vec4 0))))
       (vec4 (+ p.xyz (* s.www (- (* s.xyz 2) 1)))
             p.w))}
     snoise4D
     {([vec4] float)
      ([v]
       (=vec2 C (vec2 0.138196601125010504 0.309016994374947451))

       (=vec4 i (floor (+ v (dot v C.yyyy))))
       (=vec4 x0 (+ (dot i C.xxxx)
                    (- v i)))
       (=vec4 c0 (vec4 0))
       (=vec3 isX (step x0.yzw x0.xxx))
       (=vec3 isYZ (step x0.zww x0.yyz))

       (= c0.x (+ isX.x isX.y isX.z))
       (= c0.yzw (- 1 isX))

       (+= c0.y (+ isYZ.x isYZ.y))
       (+= c0.zw (- 1 isYZ.xy))

       (+= c0.z isYZ.z)
       (+= c0.w (- 1 isYZ.z))

       (=vec4 c3 (clamp c0 0 1))
       (=vec4 c2 (clamp (- c0 1) 0 1))
       (=vec4 c1 (clamp (- c0 2) 0 1))

       (=vec4 x1 (+ C.xxxx (- x0 c1)))
       (=vec4 x2 (+ (* 2 C.xxxx) (- x0 c2)))
       (=vec4 x3 (+ (* 3 C.xxxx) (- x0 c3)))
       (=vec4 x4 (+ (* 4 C.xxxx) (- x0 1)))

       (= i (mod i 289))

       (=float j0 (permute (+ i.x
                              (permute (+ i.y
                                          (permute (+ i.z
                                                      (permute i.w))))))))
       (=vec4 j1
              (permute
               (+ i.x
                  (vec4 c1.x c2.x c3.x 1)
                  (permute (+ i.y
                              (vec4 c1.y c2.y c3.y 1)
                              (+ (permute (+ i.z
                                             (vec4 c1.z c2.z c3.z 1)
                                             (permute (+ i.w
                                                         (vec4 c1.w
                                                               c2.w
                                                               c3.w
                                                               1)))))))))))
       (=vec4 ip (vec4 0.003401360544217687
                       0.02040816326530612
                       0.14285714285714285
                       0))

       (=vec4 p0 (grad4 j0 ip))
       (=vec4 p1 (grad4 j1.x ip))
       (=vec4 p2 (grad4 j1.y ip))
       (=vec4 p3 (grad4 j1.z ip))
       (=vec4 p4 (grad4 j1.w ip))

       (=vec4 norm (taylorInvSqrt (vec4 (dot p0 p0)
                                        (dot p1 p1)
                                        (dot p2 p2)
                                        (dot p3 p3))))
       (*= p0 norm.x)
       (*= p1 norm.y)
       (*= p2 norm.z)
       (*= p3 norm.w)
       (*= p4 (taylorInvSqrt (dot p4 p4)))

       (=vec3 m0 (max (- 0.6 (vec3 (dot x0 x0)
                                   (dot x1 x1)
                                   (dot x2 x2)))
                      0))
       (=vec2 m1 (max (- 0.6 (vec2 (dot x3 x3)
                                   (dot x4 x4)))
                      0))
       (*= m0 m0)
       (*= m1 m1)
       (* 49
          (+ (dot (* m0 m0)
                  (vec3 (dot p0 x0)
                        (dot p1 x1)
                        (dot p2 x2)))
             (dot (* m1 m1)
                  (vec2 (dot p3 x3)
                        (dot p4 x4))))))}}})

; based on https://gamedev.stackexchange.com/a/23639

(def tileable-simplex-2d-chunk
  (merge-chunks
   simplex-4d-chunk
   (postwalk-replace
    {:TAU (.toFixed (* Math/PI 2) 12)}
    '{:functions
      {snoiseTileable2D
       {([vec2 vec2 vec2] float)
        ([basePos scale pos]
         (=vec2 angles (* pos :TAU))
         (snoise4D
          (+ basePos.xyxy
             (* (vec4 (cos angles)
                      (sin angles))
                (.xyxy (/ scale :TAU))))))}}})))

; fractional brownian motion
(defn get-fbm-chunk [noise-fn noise-dimensions & noise-prefix-args]
  (postwalk-replace
   {:noise-expression (concat (list noise-fn)
                              noise-prefix-args
                              '((* f x)))
    :vec ({1 'float
           2 'vec2
           3 'vec3
           4 'vec4}
          noise-dimensions)}
   '{:functions
     {fbm
      {([:vec int float] float)
       ([x octaves hurstExponent]
        (=float g (exp2 (- 0 hurstExponent)))
        (=float f 1)
        (=float a 1)
        (=float t 0)
        ("for(int i=0;i<octaves;i++)"
         (+= t (* a :noise-expression))
         (*= f 2)
         (*= a g))
        t)}}}))

;based on www.shadertoy.com/view/Xd23Dh
(def voronoise-chunk
  '{:functions {hash3 {([vec2] vec3)
                       ([p]
                        (=vec3 q (vec3 (dot p (vec2 127.1 311.7))
                                       (dot p (vec2  269.5 183.3))
                                       (dot p (vec2 419.2 371.9))))
                        (fract (* (sin q) 43758.5453)))}
                voronoise {([float float vec2] float)
                           ([skew blur p]
                            (=float k (+ 1 (* 63 (pow (- 1 blur) 6))))

                            (=vec2 i (floor p))
                            (=vec2 f (fract p))

                            (=vec2 a (vec2 0))
                            ("for(int y=-2; y<=2; y++)"
                             ("for(int x=-2; x<=2; x++)"
                              (=vec2 g (vec2 x y))
                              (=vec3 o (* (hash3 (+ i g))
                                          (vec3 skew skew 1)))
                              (=vec2 d (- g (+ f o.xy)))
                              (=float w (pow (- 1
                                                (smoothstep 0
                                                            1.414
                                                            (length d)))
                                             k))
                              (+= a (vec2 (* o.z w) w))))
                            (/ a.x a.y))}}})
