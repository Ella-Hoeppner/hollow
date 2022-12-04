(ns sprog.tools.camera)

(defn perspective-matrix [left right bottom top near far]
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
