(ns sprog.iglu.chunks.transformations)

(def x-rotation-matrix-chunk
  {:signatures '{xRotationMatrix ([float] mat3)}
   :functions
   {'xRotationMatrix
    '([angle]
      (mat3 1 0 0
            0 (cos angle) (- "0.0" (sin angle))
            0 (sin angle) (cos angle)))}})

(def y-rotation-matrix-chunk
  {:signatures '{yRotationMatrix ([float] mat3)}
   :functions
   {'yRotationMatrix
    '([angle]
      (mat3 (cos angle) 0 (sin angle)
            0 1 0
            (- "0.0" (sin angle)) 0 (cos angle)))}})

(def z-rotation-matrix-chunk
  {:signatures '{zRotationMatrix ([float] mat3)}
   :functions
   {'zRotationMatrix
    '([angle]
      (mat3 (cos angle) (sin angle) 0
            (- "0.0" (sin angle)) (cos angle) 0
            0 0 1))}})
