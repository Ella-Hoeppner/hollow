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
