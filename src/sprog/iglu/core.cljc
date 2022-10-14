(ns sprog.iglu.core
  (:require [sprog.util :as u]
            [sprog.iglu.glsl :refer [parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.chunks.misc :refer [apply-macros]]
            [clojure.walk :refer [postwalk-replace]]))

(defn merge-chunks [& chunks]
  (assoc (reduce (partial merge-with merge)
                 (map #(dissoc % :version) chunks))
         :version "300 es"))

(defn iglu->glsl
  ([shader]
   (-> shader parse parsed-iglu->glsl))
  ([replacement-and-macro-map & chunks]
   (let [{macros true
          replacements false}
         (group-by (comp fn? second) replacement-and-macro-map)]
     (iglu->glsl
      (postwalk-replace (into {} replacements)
                        (apply-macros (into {} macros)
                                      (apply merge-chunks chunks)))))))
