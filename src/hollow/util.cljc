(ns hollow.util
  #?(:clj (:require [clojure.walk :refer [prewalk-replace
                                          prewalk]]))
  #?(:cljs (:require-macros [hollow.util])))

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

#_(defn pretty-log [& vals]
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

(defn degrees->radians [degrees]
  (* (/ Math/PI 180) degrees))

(defn uni->bi [x] (dec (* x 2)))

(defn bi->uni [x] (* 0.5 (inc x)))

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
