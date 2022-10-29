(ns sprog.iglu.glsl
  (:require [clojure.string
             :refer [join
                     starts-with?
                     ends-with?
                     includes?
                     replace]
             :rename {replace string-replace}]
            [clojure.walk :refer [walk]]
            [clojure.set :refer [union
                                 intersection]]
            [sprog.iglu.parse :refer [int-literal?]]))

(defn parse-int [s]
  #?(:clj (Integer/ParseInt))
  #?(:cljs (js/parseInt s)))

(defn symbol->glsl-str [sym]
  (string-replace (str sym) "-" "_"))

;; multimethods

(defmulti ->function-call
  (fn [fn-name args]
    (cond
      (number? fn-name) ::number
      (int-literal? fn-name) ::int-literal
      ('#{? if} fn-name) ::inline-conditional
      ('#{+ - * / % < > <= >= == != || && "^^"} fn-name) ::operator
      ('#{= += -= *= "/="} fn-name) ::assignment
      (#{"if" "else if" "switch" "for" "while"} fn-name) ::block-with-expression
      (string? fn-name) ::block
      (-> fn-name str (starts-with? "=")) ::local-assignment
      (-> fn-name str (starts-with? ".")) ::property
      :else fn-name)))

(defmulti ->subexpression
  (fn [val] (first val)))

(defmulti ->statement
  (fn [val] (first val)))

;; ->function-call

(defmethod ->function-call ::assignment [fn-name args]
  (when-not (= 2 (count args))
    (throw (ex-info (str fn-name " requires 2 args") {})))
  (let [[sym val] args]
    (str (->subexpression sym) " " fn-name " " (->subexpression val))))

(defmethod ->function-call ::local-assignment [fn-name args]
  (when-not (= 2 (count args))
    (throw (ex-info (str fn-name " requires 2 args") {})))
  (let [[sym val] args]
    (str
     (-> fn-name str (subs 1))
     " "
     (->subexpression sym)
     " = "
     (->subexpression val))))

(defmethod ->function-call ::block-with-expression [fn-name args]
  (when (< (count args) 1)
    (throw (ex-info (str fn-name " requires 1 arg") {})))
  (let [[condition & body] args]
    (cond-> (str fn-name " " (->subexpression condition))
      (seq body)
      (cons (mapv ->statement body)))))

(defmethod ->function-call ::block [fn-name args]
  (when (< (count args) 1)
    (throw (ex-info (str fn-name " requires 1 arg") {})))
  (cons fn-name (mapv ->statement args)))

(defmethod ->function-call ::inline-conditional [fn-name args]
  (when-not (= 3 (count args))
    (throw (ex-info (str fn-name " requires 3 args") {})))
  (let [[condition true-case false-case] args]
    (str
     (->subexpression condition)
     " ? " (->subexpression true-case)
     " : " (->subexpression false-case))))

(defmethod ->function-call ::operator [fn-name args]
  (join (str " " fn-name " ") (mapv ->subexpression args)))

(defmethod ->function-call ::property [fn-name args]
  (when (not= (count args) 1)
    (throw (ex-info (str fn-name " requires exactly one arg") {})))
  (str (-> args first ->subexpression) "." (-> fn-name str (subs 1))))

(defmethod ->function-call ::number [fn-name args]
  (when (not= (count args) 1)
    (throw (ex-info (str fn-name " requires exactly one arg") {})))
  (str (->subexpression (first args)) "[" fn-name "]"))

(defmethod ->function-call ::int-literal [fn-name args]
  (when (not= (count args) 1)
    (throw (ex-info (str fn-name " requires exactly one arg") {})))
  (str (->subexpression (first args)) "[" fn-name "]"))

(defmethod ->function-call :default [fn-name args]
  (str fn-name "(" (join ", " (mapv ->subexpression args)) ")"))

;; ->statement

(defmethod ->statement :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression]
    (->function-call fn-name args)))

(defmethod ->statement :default [val]
  (->subexpression val))

;; ->expression

(defmethod ->subexpression :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression
        ret (->function-call fn-name args)]
    (if (string? ret)
      (str "(" ret ")")
      ret)))

(defmethod ->subexpression :accessor [[_ expression]]
  (let [{:keys [fn-name args]} expression]
    (->> args
         (map #(str "[" (->subexpression %) "]"))
         join
         (str fn-name))))

(defmethod ->subexpression :number [[_ number]]
  (let [num-str (str number)]
    (if (includes? num-str ".")
      num-str
      (str num-str "."))))

(defmethod ->subexpression :int-literal [[_ literal]]
  (parse-int (subs (str literal) 1)))

(defmethod ->subexpression :symbol [[_ symbol]]
  (symbol->glsl-str symbol))

(defmethod ->subexpression :string [[_ string]]
  string)

;; var definitions

(defn- parse-type [[k v]]
  (case k
    :type-name v
    :array (str (:type-name v) "[" (:size v) "]")))

(defn ->precision [[type precision]]
  (str "precision " precision " " type))

(defn ->uniform [[name type]]
  (str "uniform " (parse-type type) " " name))

(defn ->attribute [[name type]]
  (str "attribute " (parse-type type) " " name))

(defn ->varying [[name type]]
  (str "varying " (parse-type type) " " name))

(defn ->inout [in-or-out qualifiers [name type]]
  (str (when qualifiers
         (let [qualifier (qualifiers name)]
           (when qualifier (str qualifier " "))))
       in-or-out
       " "
       (parse-type type)
       " "
       (symbol->glsl-str name)))

(defn ->in [qualifiers name-type-pair]
  (->inout "in" qualifiers name-type-pair))

(defn ->out [qualifiers name-type-pair]
  (->inout "out" qualifiers name-type-pair))

(defn ->struct [[name fields]]
  (str "struct "
       name
       "{\n"
       (apply str
              (map (fn [[field-name field-type]]
                     (str "  " field-type " " field-name ";\n"))
                   (partition 2 fields)))
       "}"))

(defn ->function [signatures [name {:keys [args body]}]]
  (let [signature (if (= (str name) "main")
                    '{:in [] :out void}
                    (get signatures name))]
    (when-not signature
      (throw
       (ex-info "Nothing found in :signatures for function"
                {:fn name})))
    (let [{:keys [in out]} signature]
      (when (not= (count in) (count args))
        (throw (ex-info (str "Function has args signature of a different"
                             " length than its args definition")
                        {:fn name
                         :signature in
                         :definition args})))
      (let [args-list (join ", "
                                (mapv (fn [type name]
                                        (str type " " name))
                                      in args))
            signature (str out " " name "(" args-list ")")]
        (into [signature]
              (let [body-lines (mapv ->statement body)]
                (if (= 'void out)
                  body-lines
                  (conj
                   (vec (butlast body-lines))
                   (str "return " (last body-lines))))))))))

;; compiler fn

(defn indent [level line]
  (str (join (repeat (* level 2) " "))
       line))

(defn stringify [level lines line]
  (cond
    (string? line)
    (conj lines
          (if (or (starts-with? line "#")
                  (ends-with? line ";"))
            line
            (str (indent level line) ";")))
    (string? (first line))
    (-> lines
        (conj (indent level (first line)))
        (conj (indent level "{"))
        (into (reduce (partial stringify (inc level)) [] (rest line)))
        (conj (indent level "}")))
    :else
    (into lines (reduce (partial stringify level) [] line))))

(defn sort-fns [functions]
  (letfn [(inner-symbols [form]
                         (walk (fn [s]
                                 (if (coll? s)
                                   (if (map? s)
                                     (inner-symbols (vals s))
                                     (inner-symbols s))
                                   (if (symbol? s)
                                     #{s}
                                     #{})))
                               #(apply union %)
                               form))]
    (let [fn-names (set (keys functions))
          fn-deps (into {}
                        (mapv (fn [[fn-name fn-content]]
                                [fn-name
                                 (intersection fn-names
                                               (inner-symbols
                                                (:body fn-content)))])
                              functions))]
      (loop [remaining-names fn-names
             sorted-fns []]
        (if (empty? remaining-names)
          (seq sorted-fns)
          (let [next-fn-name (some #(when (empty?
                                           (intersection remaining-names
                                                         (fn-deps %)))
                                      %)
                                   remaining-names)]
            (if next-fn-name
              (recur (disj remaining-names next-fn-name)
                     (conj sorted-fns [next-fn-name (functions next-fn-name)]))
              (throw (ex-info "Cyclic dependency detected between functions"
                              {:functions (str remaining-names)})))))))))

(defn parsed-iglu->glsl [{:keys [version
                                 precision
                                 uniforms
                                 structs
                                 attributes
                                 varyings
                                 inputs
                                 outputs
                                 qualifiers
                                 signatures
                                 main
                                 functions]}]
  (let [full-functions (cond-> functions
                         main (assoc 'main {:args [] :body main}))
        sorted-fns (sort-fns full-functions)]
    (->> (into (cond-> []
                 version (conj (str "#version " version))
                 precision (into (mapv ->precision precision))
                 uniforms (into (mapv ->uniform uniforms))
                 attributes (into (mapv ->attribute attributes))
                 varyings (into (mapv ->varying varyings))
                 inputs (into (mapv (partial ->in qualifiers) inputs))
                 outputs (into (mapv (partial ->out qualifiers) outputs))
                 structs (into (mapv ->struct structs)))
               (mapv (partial ->function signatures)
                     sorted-fns))
         (reduce (partial stringify 0) [])
         (join \newline))))
