(ns sprog.iglu.chunks.sdf
  (require [sprog.iglu.chunks.raytracing :refer [ray-chunk]]
           [sprog.iglu.core :refer [merge-chunks]]))

(def plane-sdf-chunk
  (merge-chunks ray-chunk
                '{:functions {sdPlane
                              {([vec3 Ray] float)
                               ([pos planeRay]
                                (dot (- planeRay.pos pos) planeRay.dir))}}}))

; SDFs based on https://iquilezles.org/articles/distfunctions/

; exact SDFs
(def sphere-sdf-chunk
  '{:functions {sphereSDF {([vec3 vec3 float] float)
                           ([pos spherePos radius]
                            (- (length (- pos spherePos)) radius))}}})

(def box-sdf-chunk
  '{:functions {boxSDF {([vec3 vec3 vec3] float)
                        ([pos boxPos boxDim]
                         (=vec3 q (- (abs (- pos boxPos)) boxDim))
                         (+ (length (max q 0))
                            (min (max q.x (max q.y q.z)) 0)))}}})

(def rounded-box-sdf-chunk
  '{:functions {rboxSDF {([vec3 vec3 vec3 float] float)
                         ([pos boxPos boxDim roundConst]
                          (=vec3 q (- (abs (- pos boxPos)) boxDim))
                          (- (+ (length (max q 0))
                                (min (max q.x (max q.y q.z)) 0)) roundConst))}}})

(def box-frame-sdf-chunk
  '{:functions {boxfSDF {([vec3 vec3 vec3 float] float)
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
  '{:functions {torusSDF {([vec3 vec3 vec2] float)
                          ([pos torusPos t]
                           (= pos (- pos torusPos))
                           (=vec2 q (vec2 (- (length pos.xz) t.x) pos.y))
                           (- (length q) t.y))}}})

; angle = sin/cos  of angle
(def cone-sdf-chunk  
  '{:functions {coneSDF {([vec3 vec3 vec2 float] float)
                          ([pos conePos angle height]
                           (= pos (- pos conePos))
                           (=vec2 q (* h (vec2 (/ angle.x angle.y) -1)))
                           
                           (=vec2 w (vec2 (length pos.xz) pos.y))
                           (=vec2 a (- w (*  q (clamp (/ (dot w q) 
                                                         (dot q q)) 
                                                      0 1))))
                           (=vec2 b (- w (* q (vec2 (clamp (/ w.x q.x) 0 1) 1))))
                           
                           (=float k (sign q.y))
                           (=float d (min (dot a a) (dot b b)))
                           (=float s (max (* k (- (* w.x  q.y) (* w.y q.x)))
                                          (* k (-  w.y q.y))))
                           
                           (- (sqrt d) (sign s)))}}})

(def hex-prism-sdf-chunk
  '{:functions {hexpSDF {([vec3 vec3 vec2] float)
                          ([pos hexpPos h]
                           (= pos (abs (- pos hexpPos)))
                           (=vec3 k (vec3 -0.8660254 0.5 0.57735))
                           (-= pos.xy (* 2 (min (dot k.xy  pos.xy) 0) k.xy))
                           
                           (=vec2 d (* (length (- pos.xy 
                                                  (vec2 (clamp pos.x 
                                                               (* -k.z h.x)
                                                               (* k.z h.x))))
                                               h.x)
                                       (sign (- pos.y h.x) (- pos.z h.y))))
                           
                           (+ (min (max d.x d.y) 0) 
                              (length (max d 0))))}}})

; can also be used for lines
(def capsule-sdf-chunk
  '{:functions {capSDF {([vec3 vec3 vec3 vec3 float] float)
                          ([pos capPos a b r]
                           (= pos (- pos capPos))
                           (=vec3 pa (- p a))
                           (=vec3 ba (- b a))
                           
                           (=float h (clamp (/ (dot pa ba) (dot ba ba)) 0 1))
                           
                           (- (length (- pa (* ba h))) r))}}})

; vertical capsule?? can this be generalized/combined? 
; including in case it cant
(def vert-capsule-sdf-chunk
  '{:functions {capSDF {([vec3 vec3 float float] float)
                        ([pos capPos h r]
                         (= pos (- pos capPos))
                         (-= pos.y (clamp p.y 0 h))
                         
                         (- (length p) r))}}})

(def octahedron '{:functions {octaSDF
                              {([vec3 vec3 float] float)
                               ([pos octaPos s]
                                (= pos (abs (- pos octaPos)))

                                (=float m (+ pos.x pos.y (- pos.z s)))
                                (=vec3 q (if (< (* 3 pos.x) m) pos
                                             (if (< (* 3 pos.y) yzx)
                                               (if (< (* 3 pos.z) m)
                                                 pos.zxy
                                                 (* m 0.57735027)))))

                                (=float k (clamp (* 0.5
                                                    (- q.x (+ q.y s)))
                                                 0 s))
                                (length (vec3 q.x (- q.y (+ s k))
                                              (- q.z k))))}}})




