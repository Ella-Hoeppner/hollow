(ns sprog.util
  (:require [clojure.walk :refer [prewalk
                                  prewalk-replace]]))

(defmacro unquotable [expression]
  (let [quote-replacement (gensym 'IGLU_REPLACED_QUOTE)]
    (letfn [(inline-unquotes
              [form]
              (let [replacement-map-atom (atom {})
                    inlined-replacements-form
                    (doall
                     (prewalk
                      (fn [subform]
                        (if (and (list? subform)
                                 (= (first subform)
                                    'clojure.core/unquote))
                          (let [replacement-binding (keyword (gensym))]
                            (swap! replacement-map-atom
                                   assoc
                                   replacement-binding
                                   (second subform))
                            replacement-binding)
                          subform))
                      form))]
                (list 'clojure.walk/prewalk-replace
                      @replacement-map-atom
                      (list `quote
                            (replace-quotes inlined-replacements-form)))))
            (replace-quotes
              [form]
              (if (and (list? form)
                       (= (first form)
                          quote-replacement))
                (let [subform (second form)]
                  (if (coll? subform)
                    (inline-unquotes subform)
                    (list `quote subform)))
                form))]
      (->> expression
           (prewalk-replace {`quote quote-replacement})
           (prewalk replace-quotes)))))
