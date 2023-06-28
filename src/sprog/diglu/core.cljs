(ns sprog.diglu.core
  (:require [sprog.util :as u]
            [sprog.iglu.core :as old-iglu-core]
            [sprog.diglu.compiler :refer [processed-iglu->glsl]]
            [clojure.walk :refer [prewalk-replace]]))

(def combine-chunks old-iglu-core/combine-chunks)

(def apply-macros old-iglu-core/apply-macros)

(def gensym-replace old-iglu-core/gensym-replace)

(defn preprocess [{:keys [constants main] :as shader}]
  (-> shader
      (cond-> main (update :functions
                           assoc
                           'main
                           (conj main [] 'void)))
      (cond->> constants (prewalk-replace constants))
      (dissoc :main :constants)
      apply-macros))

(defn iglu->glsl
  ([shader] (->> shader
                 preprocess
                 processed-iglu->glsl))
  ([first-chunk & other-chunks]
   (iglu->glsl (apply combine-chunks (cons first-chunk other-chunks)))))
