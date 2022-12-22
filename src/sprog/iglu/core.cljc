(ns sprog.iglu.core
  (:require [clojure.walk :refer [prewalk
                                  prewalk-replace]]
            [sprog.iglu.glsl :refer [clj-name->glsl-name
                                     parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.macros :refer [apply-macros
                                       default-macros]]))

(defn combine-chunks [& chunks]
  (reduce (partial merge-with merge) chunks))

(defn preprocess [{:keys [constants macros] :as shader}]
  (let [[macroexpanded-shader macro-chunks]
        (apply-macros (merge macros default-macros)
                      (dissoc shader :macros))]
    (cond->> (apply combine-chunks (cons macroexpanded-shader macro-chunks))
     constants (prewalk-replace constants))))

(defn iglu->glsl
  ([shader] (->> shader
                 preprocess
                 parse
                 parsed-iglu->glsl))
  ([first-chunk & other-chunks]
   (iglu->glsl (apply combine-chunks (cons first-chunk other-chunks)))))

(defn inline-float-uniforms [numerical-param-names & chunks]
  (let [param-uniform-names
        (apply hash-map
               (mapcat #(list % (clj-name->glsl-name %))
                       numerical-param-names))]
    (prewalk-replace param-uniform-names
                     (apply combine-chunks
                            (concat chunks
                                    (list
                                     {:uniforms
                                      (zipmap (vals param-uniform-names)
                                              (repeat 'float))}))))))
