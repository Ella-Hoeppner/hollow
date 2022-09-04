(ns sprog.util
  #?(:cljs (:require-macros [sprog.util])))

(defn now []
  #?(:cljs (js/Date.now)
     :clj (System/currentTimeMillis)))

(defn map-keys [f map-keys]
  (reduce #(assoc %1
                  %2
                  (f %2))
          {}
          map-keys))

(defn log [& vals]
  (doseq [val vals]
    #?(:cljs (js/console.log (str val))
       :clj (prn val)))
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
   (defmacro generate
     ([exp]
      `(repeatedly (fn [] ~exp)))
     ([number exp]
      `(repeatedly ~number (fn [] ~exp)))))
