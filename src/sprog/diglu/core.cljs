(ns sprog.diglu.core
  (:require [sprog.util :as u]
            [sprog.iglu.core :as old-iglu-core]
            [sprog.diglu.compiler :refer [processed-iglu->glsl]]
            [clojure.walk :refer [prewalk-replace
                                  prewalk]]
            [sprog.iglu.macros :refer [default-macros]]))

(defn combine-chunks [& chunks]
  (let [merged-functions
        (apply (partial merge-with
                        (fn [body-1 body-2]
                          (vec (concat
                                (if (vector? body-1) body-1 (list body-1))
                                (if (vector? body-2) body-2 (list body-2))))))
               (map :functions chunks))]
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

(defn gensym-replace [replacements expression]
  (prewalk-replace
   (into {}
         (map (fn [k]
                [k (gensym (symbol k))])
              replacements))
   expression))

(defn strip-redefines [shader]
  (update
   shader
   :functions
   (fn [functions]
     (into {}
           (map (fn [[fn-name fn-body]]
                  [fn-name
                   (if (vector? fn-body)
                     (first
                      (reduce
                       (fn [[included-definitions
                             encountered-signatures]
                            fn-definition]
                         (let [signature [(first fn-definition)
                                          (partition 1
                                                     2
                                                     (rest
                                                      (second fn-definition)))]]
                           (if (encountered-signatures signature)
                             [included-definitions
                              encountered-signatures]
                             [(conj included-definitions fn-definition)
                              (conj encountered-signatures signature)])))
                       [[] #{}]
                       fn-body))
                     fn-body)])
                functions)))))

(defn preprocess [{:keys [constants main] :as shader}]
  (-> shader
      (cond-> main (update :functions
                           assoc
                           'main
                           (conj main [] 'void)))
      (cond->> constants (prewalk-replace constants))
      (dissoc :main :constants)
      apply-macros
      strip-redefines))

(defn iglu->glsl
  ([shader] (->> shader
                 preprocess
                 processed-iglu->glsl))
  ([first-chunk & other-chunks]
   (iglu->glsl (apply combine-chunks (cons first-chunk other-chunks)))))
