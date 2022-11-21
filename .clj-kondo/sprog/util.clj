(ns sprog.util
  (:require [clojure.walk :refer [prewalk]]))

(defmacro q [form]
  (let [a (atom {})
        inlined-replacements-form
        (doall
         (prewalk (fn [subform]
                    (if (and (list? subform)
                             (= (first subform) 'clojure.core/unquote))
                      (let [replacement-binding
                            (keyword (gensym 'IGLU_REPLACEMENT_BINDING))]
                        (swap! a
                               assoc
                               replacement-binding
                               (second subform))
                        replacement-binding)
                      subform))
                  form))]
    [@a inlined-replacements-form]
    (list 'clojure.walk/prewalk-replace
          @a
          (list `quote inlined-replacements-form))))