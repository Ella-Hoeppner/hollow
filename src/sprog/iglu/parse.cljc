(ns sprog.iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn int-literal? [x]
  (and (or (symbol? x)
           (str x))
       (let [x-str (str x)
             first-letter (first x-str)
             remainder (subs x-str 1)]
         (and (= first-letter \i)
              (re-matches #"[0-9]+" remainder)))))

(s/def ::type (s/or
               :type-name symbol?
               :array (s/cat :type-name symbol? :size int?)))
(s/def ::declarations (s/map-of symbol? ::type))

(s/def ::version string?)
(s/def ::precision (s/map-of symbol? symbol?))
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
(s/def ::subexpression (s/or
                        :number number?
                        :int-literal int-literal?
                        :symbol symbol?
                        :string string?
                        :accessor (s/and vector? ::expression)
                        :expression ::expression))

(s/def ::body (s/+ (s/spec ::subexpression)))
(s/def ::function (s/cat :args (s/coll-of symbol?) :body ::body))
(s/def ::signature (s/cat :in (s/coll-of symbol?) :out symbol?))
(s/def ::functions (s/map-of symbol?
                             (s/map-of ::signature
                                       ::function
                                       :conform-keys true)))
(s/def ::main ::body)

(s/def ::shader (s/keys :opt-un [::version
                                 ::precision
                                 ::qualifiers
                                 ::uniforms
                                 ::attributes
                                 ::varyings
                                 ::inputs
                                 ::outputs
                                 ::main
                                 ::functions]))

(defn parse [content]
  (let [parsed-content (s/conform ::shader content)]
    (if (= parsed-content ::s/invalid)
      (throw (ex-info (expound/expound-str ::shader content) {}))
      parsed-content)))
