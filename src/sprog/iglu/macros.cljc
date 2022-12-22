(ns sprog.iglu.macros
  (:require [clojure.walk :refer [prewalk]]))

(defn apply-macros [macro-map expression]
  (let [chunks (atom nil)]
    [(doall
      (prewalk (fn [subexp]
                 (if (list? subexp)
                   (let [macro-fn (macro-map (first subexp))]
                     (if macro-fn
                       (let [macro-result (apply macro-fn (rest subexp))]
                         (if (map? macro-result)
                           (do (swap! chunks conj (:chunk macro-result))
                               (:expression macro-result))
                           macro-result))
                       subexp))
                   subexp))
               expression))
     @chunks]))

(defn thread-first [x & forms]
  (loop [x x
         forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (concat (list (first form)
                                     x)
                               (next form))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(defn thread-last [x & forms]
  (loop [x x
         forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (concat (list (first form))
                               (next form)
                               (list x))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(def default-macros
  {'-> thread-first
   '->> thread-last})
