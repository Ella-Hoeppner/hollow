(ns sprog.iglu.macros
  (:require [clojure.walk :refer [prewalk]]))

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
   '->> thread-last
   '=-> (fn [var-name & forms]
          (list '=
                var-name
                (concat (list '->
                              var-name)
                        forms)))
   '=->> (fn [var-name & forms]
           (list '=
                 var-name
                 (concat (list '->>
                               var-name)
                         forms)))
   'bi->uni #(list '+ 1 (list '* 0.5 %))
   'uni->bi #(list '- (list '* 2 %) 1)})
