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

(defn magnitude [v]
  (Math/sqrt (apply + (map #(* % %) v))))

(defn dot [a b]
  (apply + (map * a b)))

(defn cross [a b]
  (let [[x1 y1 z1] a
        [x2 y2 z2] b]
    [(- (* y1 z2)
        (* z1 y2))
     (- (* z1 x2)
        (* x1 z2))
     (- (* x1 y2)
        (* y1 x2))]))

(defn normalize [v]
  (let [m (magnitude v)]
    (mapv #(/ % m) v)))