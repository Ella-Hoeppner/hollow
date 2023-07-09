(ns sprog.tools.models
  (:require [sprog.util :as u]
            [clojure.string :as s]))

(defn parse-face [face]
  (vec
   (flatten (map parse-long
                 (s/split face "/")))))


(defn partition-triangles [quad]
  (let [order (flatten (partition 3 1 (range 4)))]
    (mapv (fn [q]
            (mapv (fn [i]
                    (nth q i))
                  order))
          quad)))

(defn parse-obj
  "parses obj files, fairly barebones. 
   only supports triangles and quads, 
   assumes same # of vertices/normals/texcoords"
  [file]
  (let [str->param {"v" :positions
                    "vt" :texcoords
                    "vn" :normals
                    "vp" :param-space
                    "f" :indices
                    "l" :lines}
        lines (s/split file "\n")
        parsed (reduce
                (fn [acc line]
                  (let [tokens (remove #(or (empty? %)
                                            (re-matches #"(\s|\n)" %))
                                       (s/split line #"(\s|\n)"))
                        [data-type & data] tokens
                        face? (= data-type "f")
                        quads? (and face?
                                    (= (count data) 4))
                        parse-line #(conj % (mapv (if face?
                                                    parse-face
                                                    parse-double)
                                                  data))]

                    (-> acc
                        (update (str->param data-type) parse-line)
                        (assoc :quads? quads?))))
                {:positions []
                 :texcoords []
                 :normals []
                 :param-space []
                 :indices []
                 :lines []}
                lines)]
    (if (:quads? parsed)
      (update parsed :indices partition-triangles)
      parsed)))