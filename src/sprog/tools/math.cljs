(ns sprog.tools.math)

(defn rand-normals [n rand-fn]
  (take n (apply concat (repeatedly
                         (fn []
                           (let [u1 (rand-fn)
                                 u2 (rand-fn)
                                 radius (Math/sqrt (* -2 (Math/log u1)))
                                 angle (* Math/PI 2 u2)]
                             (map #(* radius (% angle))
                                  (list Math/cos Math/sin))))))))

(defn rand-n-sphere-point [n rand-fn]
  (let [normals (rand-normals n rand-fn)
        magnitude (Math/sqrt (apply + (map #(* % %) normals)))]
    (map #(/ % magnitude)
         normals)))

(defn axis-rotation-matrix [[x y z] angle]
  (let [s (Math/sin angle)
        c (Math/cos angle)
        oc (- 1 c)]
    [(+ (* oc x x) c)
     (- (* oc x y) (* z s))
     (+ (* oc z x) (* y s))

     (+ (* oc x y) (* z s))
     (+ (* oc y y) c)
     (- (* oc y z) (* x s))

     (- (* oc z x) (* y s))
     (+ (* oc y z) (* x s))
     (+ (* oc z z) c)]))

