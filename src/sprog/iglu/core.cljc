(ns sprog.iglu.core
  (:require [sprog.iglu.glsl :refer [parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.chunks :refer [merge-chunks
                                       apply-macros]]
            [clojure.walk :refer [postwalk-replace]]))

(defn iglu->glsl
  ([shader]
   (-> shader parse parsed-iglu->glsl))
  ([replacement-and-macro-map & chunks]
   (let [{macros true
          replacements false}
         (group-by (comp fn?) replacement-and-macro-map)]
     (iglu->glsl
      (postwalk-replace (into {} replacements)
                        (apply-macros (into {} macros)
                                      (apply merge-chunks chunks)))))))
