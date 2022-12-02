(ns sprog.iglu.chunks.sdf
  (:require [sprog.iglu.chunks.raytracing :refer [ray-chunk]]
            [sprog.iglu.core :refer [merge-chunks]]
            [clojure.walk :refer [postwalk-replace]]))

; SDFs based on https://iquilezles.org/articles/distfunctions/
(def plane-sdf-chunk
  (merge-chunks
   ray-chunk
   '{:functions {sdPlane
                 {([vec3 Ray] float)
                  ([pos planeRay]
                   (dot (- planeRay.pos pos) planeRay.dir))}}}))

(def sphere-sdf-chunk
  '{:functions {sdSphere {([vec3 vec3 float] float)
                          ([pos spherePos radius]
                           (- (length (- pos spherePos)) radius))}}})

(def box-sdf-chunk
  '{:functions {sdBox {([vec3 vec3 vec3] float)
                       ([pos boxPos boxDim]
                        (=vec3 q (- (abs (- pos boxPos)) boxDim))
                        (+ (length (max q 0))
                           (min (max q.x (max q.y q.z)) 0)))}}})

(def box-frame-sdf-chunk
  '{:functions {sdBoxframe {([vec3 vec3 vec3 float] float)
                            ([pos boxPos boxDim width]
                             (= pos (- (abs pos) boxDim))
                             (=vec3 q (- (abs (+ (- pos boxPos) width)) width))
                             (min
                              (min (+ (length (max (vec3 pos.x q.y q.z) 0))
                                      (min (max pos.x (max q.y q.z)) 0))
                                   (+ (length (max (vec3 q.x pos.y q.z) 0))
                                      (min (max q.x (max pos.y q.z)) 0)))
                              (+ (length (max (vec3 q.x q.y pos.z) 0))
                                 (min (max q.x (max q.y pos.z)) 0))))}}})

(def torus-sdf-chunk
  '{:functions {sdTorus {([vec3 vec3 vec2] float)
                         ([pos torusPos t]
                          (= pos (- pos torusPos))
                          (=vec2 q (vec2 (- (length pos.xz) t.x) pos.y))
                          (- (length q) t.y))}}})

(def capped-torus-sdf-chunk '{:functions {sdCappedTorus
                                          {([vec3 vec3 vec2
                                             float float] float)
                                           ([pos torusPos sc ra rb]
                                            (= pos (- pos torusPos))
                                            (= pos.x (abs pos.x))

                                            (=float k (if (> (* sc.y pos.x)
                                                             (* sc.x pos.y))
                                                        (dot pos.xy sc)
                                                        (length pos.xy)))

                                            (- (sqrt (- (+ (dot pos pos)
                                                           (* ra ra))
                                                        (* 2 ra k))) rb))}}})

(def link-sdf-chunk '{:functions {sdLink {([vec3 vec3
                                            float float float]  float)
                                          ([pos linkPos le  r1 r2]
                                           (= pos (- pos linkPos))
                                           (=vec3 q (vec3 pos.x
                                                          (max (- (abs pos.y) le)
                                                               0)
                                                          pos.z))

                                           (- (length (- (vec2 (- (length q.xy)
                                                                  r1) q.z)
                                                         r2))))}}})

(def capped-cylinder-chunk '{:functions
                             {sdCylinder {([vec3 vec3 vec3
                                            vec3 float] float)
                                          ([pos cylinderPos a b radius]
                                           (=vec3 ba (- b a))
                                           (=vec3 pa (- pos a))
                                           (=float baba (dot ba ba))
                                           (=float paba (dot pa ba))
                                           (=float x (- (length
                                                         (- (* pa baba)
                                                            (* ba paba)))
                                                        (* radius baba)))
                                           (=float y (- (abs (- paba (* baba 0.5)))
                                                        (* baba 0.5)))
                                           (=float x2 (* x x))
                                           (=float y2 (* y y baba))
                                           (=float d (if (< (max x y) 0)
                                                       (* -1 (min x2 y2))
                                                       (+ (if (> x 0) x2 0)
                                                          (if (> y 0) y2 0))))

                                           (/ (* (sign d)
                                                 (sqrt (abs d)))
                                              baba))}}})

; angle = sin/cos  of angle
(def cone-sdf-chunk
  '{:functions {sdCone {([vec3 vec3 vec2 float] float)
                        ([pos conePos angle height]
                         (= pos (- pos conePos))
                         (=vec2 q (* height (vec2 (/ angle.x angle.y) -1)))

                         (=vec2 w (vec2 (length pos.xz) pos.y))
                         (=vec2 a (- w (*  q (clamp (/ (dot w q)
                                                       (dot q q))
                                                    0 1))))
                         (=vec2 b (- w (* q (vec2 (clamp (/ w.x q.x) 0 1) 1))))

                         (=float k (sign q.y))
                         (=float d (min (dot a a) (dot b b)))
                         (=float s (max (* k (- (* w.x  q.y) (* w.y q.x)))
                                        (* k (-  w.y q.y))))

                         (* (sqrt d) (sign s)))}}})

(def hex-prism-sdf-chunk
  '{:functions {sdHexprism {([vec3 vec3 vec2] float)
                            ([pos hexpPos h]
                             (= pos (abs (- pos hexpPos)))
                             (=vec3 k (vec3 -0.8660254 0.5 0.57735))
                             (-= pos.xy (* 2 (min (dot k.xy  pos.xy) 0) k.xy))

                             (=vec2 d (vec2 (* (length (- pos.xy
                                                          (vec2 (clamp pos.x
                                                                       (* (- 0 k.z) h.x)
                                                                       (* k.z h.x))
                                                                h.x)))
                                               (sign (- pos.y h.x)))
                                            (- pos.z h.y)))

                             (+ (min (max d.x d.y) 0)
                                (length (max d 0))))}}})

(def capsule-sdf-chunk
  '{:functions {sdCapsule {([vec3 vec3 vec3 vec3 float] float)
                           ([pos capPos a b r]
                            (= pos (- pos capPos))
                            (=vec3 pa (- pos a))
                            (=vec3 ba (- b a))

                            (=float h (clamp (/ (dot pa ba) (dot ba ba)) 0 1))

                            (- (length (- pa (* ba h))) r))}}})


;TODO fix this, cannot figure out what's wrong w it -Fay
#_(def octahedron-sdf-chunk '{:functions {sdOctahedron
                                        {([vec3 vec3 float] float)
                                         ([pos octaPos s]
                                          (= pos (abs (- pos octaPos)))

                                          (=float m (- (+ pos.x pos.y pos.z) s))
                                          (=vec3 q (vec3 0))
                                          ("if" (< (* pos.x 3) m)
                                                (= q pos))
                                          ("else if" (< (* pos.y 3) m)
                                                     (= q pos.yzx))
                                          ("else if" (< (* pos.z 3) m)
                                                     (= q pos.zxy))
                                          ("else" (return (* m 0.57735027)))

                                          (=float k (clamp (* 0.5 (- q.z (+ q.y s)))
                                                           0
                                                           s))
                                          (length (vec3 q.x
                                                        (- q.y (+ s k))
                                                        (- q.z k))))}}})

(def pyramid-sdf-chunk '{:functions
                         {sdPyramid {([vec3 vec3 float] float)
                                     ([pos pyramidPos h]
                                      (= pos (- pos pyramidPos))

                                      (=float m2 (+ (* h h) 0.25))

                                      (= pos.xz (abs pos.xz))
                                      (= pos.xz (if (> pos.z pos.x)
                                                  pos.zx
                                                  pos.xz))
                                      (-= pos.xz 0.5)

                                      (=vec3 q  (vec3 pos.z
                                                      (- (* h pos.y)
                                                         (* 0.5 pos.x))
                                                      (+ (* h pos.x)
                                                         (* 0.5 pos.y))))

                                      (=float s (max (* -1 q.x) 0))
                                      (=float t (clamp (/ (- q.y (* 0.5 pos.z))
                                                          (+ m2 0.25))
                                                       0 1))

                                      (=float a (+ (* m2
                                                      (+ q.x s)
                                                      (+ q.x s))
                                                   (* q.y q.y)))
                                      (=float b (+ (* m2
                                                      (+ q.x (* 0.5 t))
                                                      (+ q.x (* 0.5 t)))
                                                   (* (- q.y (* m2 t))
                                                      (- q.y (* m2 t)))))

                                      (=float d2 (if (> (min q.y (- (* -1 q.x m2)
                                                                    (* q.y 0.5)))
                                                        0)
                                                   0
                                                   (min a b)))

                                      (sqrt (* (/ (+ d2 (* q.z q.z)) m2)
                                               (sign (max q.z (- 0 pos.y))))))}}})



(defn get-multi-dimension-elongate-chunk [sdf-fn-name]
  (postwalk-replace
   {:fn-name sdf-fn-name}
   '{:functions {opElongate
                 {([vec3 vec3 vec3] float)
                  ([pos shapePos h]
                   (= pos (- pos shapePos))
                   (=vec3 q (- pos (clamp p
                                          (* -1 h)
                                          h)))

                   (:fn-name q  #_(args here)))}}}))

; combinations

(def sdf-union-chunk '{:functions
                       {opUnion
                        {([float float] float)
                         ([d1 d2]
                          (min d1 d2))}}})

(def sdf-smooth-union-chunk '{:functions
                              {opSmoothUnion
                               {([float float float]
                                 float)
                                ([d1 d2 smooth]
                                 (=float h (clamp (+ 0.5
                                                     (* 0.5 (/ (- d2 d1)
                                                               k)))
                                                  0 1))

                                 (- (mix d2 d1 h) (* k h (- 1 h))))}}})

(def sdf-subtraction-chunk '{:functions  {opSubtraction
                                          {([float float] float)
                                           ([d1 d2] (max (* -1 d1) d2))}}})

(def sdf-smooth-subtraction-chunk
  '{:functions
    {opSmoothUnion
     {([float float float]
       float)
      ([d1 d2 smooth]
       (=float h (clamp (- 0.5
                           (* 0.5 (/ (- d2 d1)
                                     k)))
                        0 1))

       (+ (mix d2 (* -1 d1) h) (* k h (- 1 h))))}}})

(def sdf-intersection-chunk
  '{:functions  {opIntersection
                 {([float float] float)
                  ([d1 d2] (max d1 d2))}}})

(def sdf-smooth-intersectionn-chunk
  '{:functions
    {opSmoothUnion
     {([float float float]
       float)
      ([d1 d2 smooth]
       (=float h (clamp (- 0.5
                           (* 0.5 (/ (- d2 d1)
                                     k)))
                        0 1))

       (+ (mix d2 d1 h) (* k h (- 1 h))))}}})