(ns sprog.kudzu.compiler
  (:require [sprog.util :as u]
            [clojure.string :refer [join]]
            [sprog.kudzu.sorting :refer [sort-fns
                                         sort-structs]]))

(defn parse-int [s]
  (js/parseInt s))

(defn num->glsl [num]
  (apply str
         (reverse
          (some #(when (not= (first %) \0)
                   %)
                (iterate rest
                         (reverse
                          (.toFixed num 20)))))))

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

(defn type->glsl [type-expression]
  (if (vector? type-expression)
    (str (clj-name->glsl (first type-expression))
         "["
         (second type-expression)
         "]")
    (clj-name->glsl type-expression)))

(def infix-ops
  '#{+ - / * % < > == <= => || && "^^" "^" "^=" | << >>})

(def modifying-assigners
  '#{+= *= -= "/="})

(defn expression->glsl [expression & [context-map]]
  (cond
    (symbol? expression) (clj-name->glsl expression)

    (number? expression) (num->glsl expression)

    (string? expression) expression

    (vector? expression)
    (if (= (count expression) 2)
      ; array accessor
      (let [[array-name array-index] expression]
        (str (expression->glsl array-name)
             "["
             (expression->glsl array-index)
             "]"))
      ; array literal
      (let [[array-type & init-values] expression]
        (str (type->glsl array-type)
             "[]("
             (join "," (map expression->glsl init-values))
             ")")))

    (seq? expression)
    (let [[f & args] expression]
      (cond
        (= '++ f) (str (expression->glsl (first args)) "++")
        (= '-- f) (str (expression->glsl (first args)) "--")

        (and (= f '-) (= (count args) 1))
        (str "-" (expression->glsl (first args)))

        (and (= f '/) (= (count args) 1))
        (str "(1./" (expression->glsl (first args)) ")")

        (modifying-assigners f)
        (str (clj-name->glsl (first args))
             " "
             f
             " "
             (expression->glsl (second args)))

        (infix-ops f)
        (str "("
             (join (str " " f " ")
                   (map expression->glsl args))
             ")")

        (= 'if f)
        (let [[conditional true-expression false-expression] args]
          (str "("
               (expression->glsl conditional)
               " ? "
               (expression->glsl true-expression)
               " : "
               (expression->glsl false-expression)
               ")"))

        (= '= f)
        (if (= (count args) 2)
          ; assignment
          (str (expression->glsl (first args))
               " = "
               (expression->glsl (second args)))
          ; 3 args, initialization with detached type
          (str (type->glsl (first args))
               " "
               (expression->glsl (second args))
               " = "
               (expression->glsl (nth args 2))))

        (and (symbol? f) (= "=" (first (str f))))
        (str (expression->glsl (symbol (subs (str f) 1)))
             " "
             (expression->glsl (first args))
             " = "
             (expression->glsl (second args)))

        (and (symbol? f) (= "." (first (str f))))
        (str (expression->glsl (first args))
             (clj-name->glsl f))

        :else (str (clj-name->glsl f)
                   "("
                   (join ", "
                         (map expression->glsl args))
                   ")")))))

(defn is-statement-block? [statement]
  (and (list? statement)
       (#{"if" "else" "for"} (first statement))))

(defn statement->lines [statement]
  (if (is-statement-block? statement)
    (let [[statement-type & statement-args] statement
          [block-start consumed-statement-args]
          (case statement-type
            "if" [(str "if("
                       (expression->glsl (first statement-args))
                       ") {")
                  1]
            "else" ["else {" 0]
            "for" [(str "for("
                        (join "; "
                              (map expression->glsl
                                   (take 3 statement-args)))
                        ") {")
                   3])]
      (concat (list (str block-start "\n"))
              (map (partial str "  ")
                   (mapcat statement->lines
                           (drop consumed-statement-args statement-args)))
              (list "}\n")))
    (list (str (expression->glsl statement) ";\n"))))

(defn int-literal? [x]
  (and (string? x)
       (re-matches #"[0-9]+" x)))

(defn precision->glsl [[glsl-type type-precision]]
  (str "precision " type-precision " " glsl-type ";\n"))

(defn uniform->glsl [[uniform-name uniform-type]]
  (str "uniform "
       (type->glsl uniform-type)
       " "
       (clj-name->glsl uniform-name)
       ";\n"))

(defn in-out->glsl [layout in-or-out [in-out-name in-out-type]]
  (str (when layout
         (when-let [layout-pos (layout in-out-name)]
           (str "layout(location = " layout-pos ") ")))
       in-or-out
       " "
       (type->glsl in-out-type)
       " "
       (clj-name->glsl in-out-name)
       ";\n"))

(defn struct->glsl [[struct-name struct-definition]]
  (str "struct "
       (clj-name->glsl struct-name)
       " {\n"
       (apply str
              (map (fn [[field-name field-type]]
                     (str " "
                          (type->glsl field-type)
                          " "
                          (clj-name->glsl field-name)
                          ";\n"))
                   (partition 2 struct-definition)))
       "};\n"))

(defn define->glsl [[define-pattern define-replacement]]
  (str "#define "
       (expression->glsl define-pattern)
       " "
       (expression->glsl define-replacement)
       "\n"))

(defn function-body->glsl [fn-body returns?]
  (let [statement-strings (vec (mapcat statement->lines fn-body))]
    (apply str
           (map (partial str "  ")
                (cond-> statement-strings
                  returns? (update (dec (count statement-strings))
                                   (partial str "return ")))))))

(defn function->glsl [[fn-name fn-definition]]
  (if (seq? fn-definition)
    (let [[return-type signature & body] fn-definition]
      (str (type->glsl return-type)
           " "
           (clj-name->glsl fn-name)
           "("
           (join ", "
                 (map (fn [[arg-name arg-type]]
                        (str (type->glsl arg-type)
                             " "
                             (clj-name->glsl arg-name)))
                      (partition 2 signature)))
           ")\n{\n"
           (function-body->glsl body (not= return-type 'void))
           "}\n"))
    (apply str
           (map #(function->glsl [fn-name %])
                fn-definition))))

(defn processed-kudzu->glsl [{:keys [precision
                                     uniforms
                                     layout
                                     inputs
                                     outputs
                                     structs
                                     defines
                                     functions]}]
  (apply str
         (flatten
          ["#version 300 es\n"
           (map precision->glsl precision)
           (map uniform->glsl uniforms)
           (map (partial in-out->glsl layout "in") inputs)
           (map (partial in-out->glsl layout "out") outputs)
           (map struct->glsl structs)
           (map define->glsl defines)
           (map function->glsl (sort-fns functions))])))
