(ns sprog.tools.models
  (:require [sprog.util :as u]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]))

;parses obj files, fairly barebones currently. 
;partitioning must be handled post-parsing, no support for groups,
;doesn't re-arrange by face-indices
(defn parse-model [file]
  (let [lines (str/split file "\n")]
    (update
     (into {}
           (map (fn [[k v]]
                  [k (vec (flatten v))])
                (reduce
                 (fn [parsed line]
                   (let [tokens (remove empty?
                                        (map #(str/replace % #"(\s|/|\n)" "")
                                             (str/split line #"(\s|/|\n)")))
                         data-type (first tokens)
                         data (rest tokens)
                         parse-line #(conj %
                                           (mapv parse-double data))]
                     (case data-type
                       "v" (update parsed :positions parse-line)
                       "vt" (update parsed :texcoords parse-line)
                       "vn" (update parsed :normals parse-line)
                       "vp" (update parsed :param-space parse-line)
                       "f" (update parsed :indices parse-line)
                       "l" (update parsed :lines parse-line)
                       parsed)))
                 {:positions []
                  :texcoords []
                  :normals []
                  :param-space []
                  :indices []
                  :lines []}
                 lines)))
     :indices
     (comp #(mapv vec %)
           (partial partition 3)))))
