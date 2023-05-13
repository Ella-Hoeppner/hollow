(ns sprog.iglu.core
  (:require [sprog.util :as u]
            [clojure.walk :refer [prewalk
                                  prewalk-replace]]
            [sprog.iglu.glsl :refer [clj-name->glsl-name
                                     parsed-iglu->glsl]]
            [sprog.iglu.parse :refer [parse]]
            [sprog.iglu.macros :refer [default-macros]]))

(defn combine-chunks [& chunks]
  (let [merged-functions (apply (partial merge-with merge) (map :functions chunks))]
    (cond-> (apply (partial merge-with merge) chunks)
      merged-functions (assoc :functions merged-functions))))

(defn apply-macros [{:keys [macros] :as shader} & [exclude-defaults?]]
  (let [chunks (atom nil)
        new-shader
        (apply combine-chunks
               (concat
                (list (prewalk
                       (fn [subexp]
                         (if (seq? subexp)
                           (let [f (first subexp)
                                 macro-fn (or (when macros
                                                (macros f))
                                              (when-not exclude-defaults?
                                                (default-macros f)))]
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
      (apply-macros new-shader exclude-defaults?))))

(defn preprocess [{:keys [constants] :as shader}]
  (-> shader
      (cond->> constants (prewalk-replace constants))
      #_(update :macros (partial merge default-macros))
      apply-macros))

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
