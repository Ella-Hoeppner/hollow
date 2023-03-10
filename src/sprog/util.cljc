(ns sprog.util
  #?(:clj (:require [clojure.walk :refer [prewalk-replace
                                          prewalk]]
                    [clojure.pprint :refer [pprint]]))
  #?(:cljs (:require [cljs.pprint :refer [pprint]]))
  #?(:cljs (:require-macros [sprog.util])))

(defn now []
  #?(:cljs (js/Date.now)
     :clj (System/currentTimeMillis)))

(def startup-time (now))
(defn seconds-since-startup [] (/ (- (now) startup-time) 1000))

(defn log [& vals]
  (doseq [val vals]
    #?(:cljs (js/console.log (str val))
       :clj (prn val)))
  (last vals))

(defn log-tables [& tables]
  (doseq [table tables]
    #?(:cljs (js/console.table (clj->js table))
       :clj (prn tables)))
  (last tables))

(defn pretty-log [& vals]
  (doseq [val vals]
         #?(:cljs (pprint val)
            :clj (pprint val)))
  (last vals))

(defn scale
  ([from-min from-max to-min to-max value]
   (+ (* (/ (- value from-min)
            (- from-max from-min))
         (- to-max to-min))
      to-min))
  ([from-min from-max to-min to-max]
   #(+ (* (/ (- % from-min)
             (- from-max from-min))
          (- to-max to-min))
       to-min))
  ([to-min to-max value]
   (scale 0 1 to-min to-max value))
  ([to-min to-max]
   (scale 0 1 to-min to-max)))

(defn prange [n & [open?]]
  (map #(/ %
           (if open?
             n
             (dec n)))
       (range n)))

(defn clamp
  ([bottom top value] (min top (max bottom value)))
  ([min max] #(clamp min max %))
  ([value] (clamp 0 1 value)))

(def sigmoid (comp / inc #(Math/exp %) -))

(def TAU (* Math/PI 2))

#?(:clj
   (defmacro gen
     ([exp]
      `(repeatedly (fn [] ~exp)))
     ([number exp]
      `(repeatedly ~number (fn [] ~exp)))))

#?(:clj
   (defmacro genv
     ([exp]
      `(vec (repeatedly (fn [] ~exp))))
     ([number exp]
      `(vec (repeatedly ~number (fn [] ~exp))))))

#?(:clj
   (defmacro unquotable [& expressions]
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
         (->> expressions
              (prewalk-replace {`quote quote-replacement})
              (prewalk replace-quotes)
              (cons 'do))))))
