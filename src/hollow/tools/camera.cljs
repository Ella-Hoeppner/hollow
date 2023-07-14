(ns hollow.tools.camera)

(defn viewport-perspective-matrix [left right bottom top near far]
  [(/ (* 2 near) (- right left))
   0
   0
   (- (/ (* near (+ right left))
         (- right left)))

   0
   (/ (* 2 near)
      (- top bottom))
   0
   (- (/ (* near (+ top bottom))
         (- top bottom)))

   0
   0
   (- (/ (+ far near)
         (- far near)))
   (/ (* 2 far near)
      (- near far))

   0
   0
   -1
   0])

(defn fov-perspective-matrix [fov aspect z-near z-far]
  (let [f (-> Math/PI
              (* 0.5)
              (- 0.5)
              (* fov)
              Math/tan)
        range-inverted (/ (- z-near z-far))]
    [(/ f aspect) 0 0 0
     0 f 0 0
     0 0 (* (+ z-near z-far) range-inverted) -1
     0 0 (* z-near z-far range-inverted 2) 0]))
