(ns sprog.iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn int-literal? [x]
  (and (string? x)
       (re-matches #"[0-9]+" x)))

(s/def ::type (s/or
               :type-name symbol?
               :array (s/cat :type-name symbol? :size int?)))
(s/def ::declarations (s/map-of symbol? ::type))

(s/def ::version string?)
(s/def ::precision (s/map-of symbol? symbol?))
(s/def ::layout (s/map-of symbol? int?))
(s/def ::qualifiers (s/map-of symbol? string?))
(s/def ::uniforms ::declarations)
(s/def ::attributes ::declarations)
(s/def ::varyings ::declarations)
(s/def ::inputs ::declarations)
(s/def ::outputs ::declarations)

(defn fn-name? [x]
  (or (symbol? x)
      (number? x)
      (string? x)))

(s/def ::expression (s/cat
                     :fn-name fn-name?
                     :args (s/* ::subexpression)))
(s/def ::subexpression
  (s/or
   :number number?
   :int-literal int-literal?
   :bool boolean?
   :symbol symbol?
   :string string?
   :array-literal (s/and vector?
                         (s/cat :type-name symbol?
                                :array-length (s/or :int-literal int-literal?
                                                    :number number?)
                                :values (s/and vector?
                                               (s/* ::subexpression))))
   :accessor (s/and vector?
                    (s/cat :array-name symbol?
                           :array-index (s/or :int-literal int-literal?
                                              :number number?)))
   :expression ::expression))

(s/def ::body (s/+ (s/spec ::subexpression)))
(s/def ::signature (s/cat :in (s/coll-of ::type) :out ::type))
(s/def ::function (s/cat :args (s/coll-of symbol?) :body ::body))
(s/def ::functions (s/map-of symbol?
                             (s/map-of ::signature
                                       ::function
                                       :conform-keys true)))
(s/def ::structs (s/map-of symbol?
                          (s/and vector?
                                 (s/coll-of ::subexpression))))
(s/def ::defines (s/map-of ::subexpression
                           ::subexpression
                           :conform-keys true))
(s/def ::main ::body)

(s/def ::shader (s/keys :opt-un [::version
                                 ::precision
                                 ::qualifiers
                                 ::layout
                                 ::uniforms
                                 ::attributes
                                 ::varyings
                                 ::inputs
                                 ::outputs
                                 ::main
                                 ::functions
                                 ::structs
                                 ::defines]))

(defn parse [content]
  (let [parsed-content (s/conform ::shader content)]
    (if (= parsed-content ::s/invalid)
      (throw (ex-info (expound/expound-str ::shader content) {}))
      parsed-content)))
