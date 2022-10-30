(ns sprog.iglu.core
  (:require [clojure.walk :refer [prewalk-replace]]
            [sprog.iglu.glsl :refer [parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.macros :refer [apply-macros
                                       default-macros]]))

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(defn iglu->glsl
  ([shader]
   (-> shader
       parse
       parsed-iglu->glsl))
  ([replacement-and-macro-map & chunks]
   (let [{macros true
          replacements false}
         (group-by (comp fn? second) replacement-and-macro-map)]
     (iglu->glsl
      (prewalk-replace (into {} replacements)
                       (apply-macros (into default-macros macros)
                                     (apply merge-chunks chunks)))))))
