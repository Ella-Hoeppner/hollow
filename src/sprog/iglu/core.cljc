(ns sprog.iglu.core
  (:require [clojure.walk :refer [prewalk
                                  prewalk-replace]]
            [sprog.iglu.glsl :refer [clj-name->glsl-name
                                     parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.macros :refer [default-macros]]))

(defn combine-chunks [& chunks]
  (assoc (apply (partial merge-with merge) chunks)
         :functions
         (apply (partial merge-with merge) (map :functions chunks))))

(defn apply-macros [macro-map shader]
  (let [chunks (atom nil)
        new-shader
        (apply combine-chunks
               (concat
                (list (prewalk
                       (fn [subexp]
                         (if (seq? subexp)
                           (let [macro-fn (macro-map (first subexp))]
                             (if macro-fn
                               (let [macro-result (apply macro-fn
                                                         (rest subexp))]
                                 (if (map? macro-result)
                                   (do (swap! chunks
                                              conj
                                              (:chunk macro-result))
                                       (:expression macro-result))
                                   macro-result))
                               subexp))
                           subexp))
                       shader))
                @chunks))]
    (if (= new-shader shader)
      new-shader
      (apply-macros macro-map new-shader))))

(defn preprocess [{:keys [constants macros] :as shader}]
  (apply-macros (merge macros default-macros)
                (dissoc (cond->> shader
                          constants (prewalk-replace constants))
                        :macros)))

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
