(ns sprog.kudzu.tools)

(defn multichar-escape [s escape-pairs]
  (loop [escaped-str ""
         remaining-str s]
    (if (empty? remaining-str)
      escaped-str
      (if-let [[new-escaped-str new-remaining-str]
               (some (fn [[replace-str replacement-str]]
                       (when (and (>= (count remaining-str)
                                      (count replace-str))
                                  (= (subs remaining-str 0 (count replace-str))
                                     replace-str))
                         [(str escaped-str replacement-str)
                          (subs remaining-str (count replace-str))]))
                     escape-pairs)]
        (recur new-escaped-str new-remaining-str)
        (recur (str escaped-str (first remaining-str))
               (subs remaining-str 1))))))

(defn clj-name->glsl [clj-name]
  (multichar-escape (cond-> (str clj-name)
                      (keyword? clj-name) (subs 1))
                    [["->" "ARROW"]
                     ["-" "_"]
                     ["?" "QUESTION_MARK"]]))
