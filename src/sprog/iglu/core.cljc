(ns sprog.iglu.core
  (:require [sprog.util :as u]
            [sprog.iglu.glsl :refer [parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]] 
            [clojure.walk :refer [prewalk-replace
                                  prewalk]]))

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(defn apply-macros [macro-map expression]
  (prewalk (fn [subexp]
              (if (vector? subexp)
                (let [macro-fn (macro-map (first subexp))]
                  (if macro-fn
                    (apply macro-fn (rest subexp))
                    subexp))
                subexp))
            expression))

(defn iglu->glsl
  ([shader]
   (-> shader parse parsed-iglu->glsl))
  ([replacement-and-macro-map & chunks]
   (let [{macros true
          replacements false}
         (group-by (comp fn? second) replacement-and-macro-map)]
     (iglu->glsl
      (prewalk-replace (into {} replacements)
                        (apply-macros (into {} macros)
                                      (apply merge-chunks chunks)))))))
