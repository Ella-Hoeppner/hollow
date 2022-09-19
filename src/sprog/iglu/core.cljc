(ns sprog.iglu.core
  (:require [sprog.iglu.glsl :refer [parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.chunks :refer [merge-chunks]]
            [clojure.walk :refer [postwalk-replace]]))

(defn iglu->glsl
  ([shader]
   (-> shader parse parsed-iglu->glsl))
  ([replacement-map & chunks]
   (iglu->glsl (postwalk-replace replacement-map (apply merge-chunks chunks)))))
