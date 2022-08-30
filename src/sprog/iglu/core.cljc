(ns sprog.iglu.core
  (:require [sprog.iglu.glsl :as glsl]
            [sprog.iglu.parse :as parse]))

(defn iglu->glsl [shader]
  (-> shader parse/parse glsl/iglu->glsl))
