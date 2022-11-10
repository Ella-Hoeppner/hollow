(ns sprog.iglu.core
  (:require [clojure.walk :refer [prewalk-replace]]
            [sprog.iglu.glsl :refer [clj-name->glsl-name
                                     parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.macros :refer [apply-macros
                                       default-macros]]))

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(defn iglu->glsl
  ([shader]
   (->> shader
        (apply-macros default-macros)
        parse
        parsed-iglu->glsl))
  ([replacement-and-macro-map & chunks]
   (let [{macros true
          replacements false}
         (group-by (comp fn? second) replacement-and-macro-map)]
     (->> chunks
          (apply merge-chunks)
          (apply-macros (into default-macros macros))
          (prewalk-replace (into {} replacements))
          iglu->glsl))))

(defn inline-float-uniforms [numerical-param-names & chunks]
  (let [param-uniform-names
        (apply hash-map
               (mapcat #(list % (clj-name->glsl-name %))
                       numerical-param-names))]
    (prewalk-replace param-uniform-names
                     (apply merge-chunks
                            (concat chunks
                                    (list
                                     {:uniforms
                                      (zipmap (vals param-uniform-names)
                                              (repeat 'float))}))))))