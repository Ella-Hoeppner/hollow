(ns sprog.iglu.chunks.transformations)

(def x-rotation-matrix-chunk
  '{:functions
    {xRotationMatrix
     {([float] mat3)
      ([angle]
       (mat3 1 0 0
             0 (cos angle) (- 0 (sin angle))
             0 (sin angle) (cos angle)))}}})

(def y-rotation-matrix-chunk
  '{:functions
    {yRotationMatrix
     {([float] mat3)
      ([angle]
       (mat3 (cos angle) 0 (sin angle)
             0 1 0
             (- 0 (sin angle)) 0 (cos angle)))}}})

(def z-rotation-matrix-chunk
  '{:functions
    {zRotationMatrix
     {([float] mat3)
      ([angle]
       (mat3 (cos angle) (sin angle) 0
             (- 0 (sin angle)) (cos angle) 0
             0 0 1))}}})

(def axis-rotation-chunk
  '{:functions
    {axisRoationMatrix
     {([vec3 float] mat3)
      ([axis angle]
       (= axis (normalize axis))
       (=float s (sin angle))
       (=float c (cos angle))
       (=float oc (- 1 c))
       (mat3 (+ (* oc axis.x axis.x) c)
             (- (* oc axis.x axis.y) (* axis.z s))
             (+ (* oc axis.z axis.x) (* axis.y s))

             (+ (* oc axis.x axis.y) (* axis.z s))
             (+ (* oc axis.y axis.y) c)
             (- (* oc axis.y axis.z) (* axis.x s))

             (- (* oc axis.z axis.x) (* axis.y s))
             (+ (* oc axis.y axis.z) (* axis.x s))
             (+ (* oc axis.z axis.z) c)))}}})
