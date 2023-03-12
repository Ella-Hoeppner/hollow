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
  #?(:clj (Integer/parseInt s))
  #?(:cljs (js/parseInt s)))

(defn num->glsl-str [num]
  (apply str
         (reverse
          (some #(when (not= (first %) \0)
                   %)
                (iterate rest
                         (reverse
                          #?(:clj (format "%.20f" num)
                             :cljs (.toFixed num 20))))))))

(defn clj-name->glsl-name [clj-name]
  (symbol
   (string-replace
    (cond-> (str clj-name)
      (keyword? clj-name) (subs 1))
    "-"
    "_")))

(defn- parse-type [[k v]]
  (case k
    :type-name v
    :array (str (:type-name v) "[" (:size v) "]")))

;; multimethods

(defmulti ->function-call
  (fn [fn-name args]
    (cond
      (= 'do fn-name) ::do-block
      (number? fn-name) ::number
      (int-literal? fn-name) ::int-literal
      ('#{? if} fn-name) ::inline-conditional
      ('#{+ - * / % < > <= >= == != || && "^^"} fn-name) ::operator
      (= '= fn-name) ::assignment
      ('#{+= -= *= "/="} fn-name) ::augment
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

(defmethod ->function-call ::do-block [_ args]
  (map ->statement args))

(defmethod ->function-call ::augment [fn-name args]
  (when-not (= 2 (count args))
    (throw (ex-info (str fn-name " requires 2 args") {})))
  (let [[sym val] args]
    (str (->subexpression sym) " " fn-name " " (->subexpression val))))

(defmethod ->function-call ::assignment [fn-name args]
  (case (count args)
    2 (let [[sym val] args]
        (str (->subexpression sym) " = " (->subexpression val)))
    3 (let [[type sym val] args]
        (str
         (->subexpression type)
         " "
         (->subexpression sym)
         " = "
         (->subexpression val)))
    (throw (ex-info (str fn-name " requires 2 args") {}))))

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
  (conj (map ->statement (rest args))
        (str fn-name " " (->subexpression (first args)))
        :block))

(defmethod ->function-call ::block [fn-name args]
  (when (< (count args) 1)
    (throw (ex-info (str fn-name " requires 1 arg") {})))
  (conj (map ->statement args) 
        fn-name
        :block))

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

(defmethod ->subexpression :accessor [[_ {:keys [array-name array-index]}]]
  (str array-name
       "["
       (let [[array-index-type array-index-value] array-index]
         (case array-index-type
           :int-literal (->subexpression array-index)
           :number (str array-index-value)))
       "]"))

(defmethod ->subexpression :number [[_ number]]
  (num->glsl-str number))

(defmethod ->subexpression :int-literal [[_ literal]]
  (parse-int (let [literal-str (str literal)]
               (if (= (first literal-str) \i)
                 (subs literal-str 1)
                 literal-str))))

(defmethod ->subexpression :symbol [[_ symbol]]
  (clj-name->glsl-name symbol))

(defmethod ->subexpression :string [[_ string]]
  string)

(defmethod ->subexpression :array-literal [[_ {:keys [type-name 
                                                      array-length 
                                                      values]}]]
  (str type-name
       "["
       (let [[array-index-type array-index-value] array-length]
         (case array-index-type
           :int-literal (->subexpression array-length)
           :number (str array-index-value)))
       "]("
       (apply str
              (rest (interleave (repeat ", ")
                                (map ->subexpression values))))
       ")"))

;; var definitions

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
       (clj-name->glsl-name name)))

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

(defn ->function [[name signature-function-map]]
  (map (fn [[signature {:keys [args body]}]]
         (let [{:keys [in out]} signature]
           (when (not= (count in) (count args))
             (throw (ex-info
                     (str "Function has args signature of a different "
                          "length than its args definition")
                     {:fn name
                      :signature in
                      :definition args})))
           (let [args-list (join ", "
                                 (mapv (fn [type name]
                                         (str type 
                                              " "
                                              (clj-name->glsl-name name)))
                                       in args))
                 signature (str out " " name "(" args-list ")")]
             (conj (seq (into [signature]
                              (let [body-lines (mapv ->statement body)]
                                (if (= 'void out)
                                  body-lines
                                  (conj
                                   (vec (butlast body-lines))
                                   (str "return " (last body-lines)))))))
                   :block))))
       signature-function-map))

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
    (= :block (first line))
    (-> lines
        (conj (indent level (second line)))
        (conj (indent level "{"))
        (into (reduce (partial stringify (inc level)) [] (drop 2 line)))
        (conj (indent level "}")))
    :else
    (into lines (reduce (partial stringify level) [] line))))

(defn inner-symbols [form]
  (walk (fn [s]
          (if (coll? s)
            (if (map? s)
              (inner-symbols (vals s))
              (inner-symbols s))
            (if (symbol? s)
              #{s}
              #{})))
        #(apply union %)
        form))

(defn sort-expressions [expressions dependencies]
  (loop [remaining-names (set (keys expressions))
         sorted-expressions []]
    (if (empty? remaining-names)
      (seq sorted-expressions)
      (let [next-expression-name (some #(when (empty?
                                               (intersection remaining-names
                                                             (dependencies %)))
                                          %)
                                       remaining-names)]
        (if next-expression-name
          (recur (disj remaining-names next-expression-name)
                 (conj sorted-expressions 
                       [next-expression-name
                        (expressions next-expression-name)]))
          (throw (ex-info "Cyclic dependency detected"
                          {:functions (str remaining-names)})))))))

(defn sort-fns [functions]
  (sort-expressions
   functions
   (into {}
         (mapv (fn [[fn-name fn-content]]
                 [fn-name
                  (intersection (set (keys functions))
                                (apply union
                                       (map #(inner-symbols
                                              (:body (second %)))
                                            fn-content)))])
               functions))))

(defn sort-structs [structs]
  (sort-expressions
   structs
   (into {}
         (mapv (fn [[struct-name struct-content]]
                 [struct-name
                  (intersection
                   (set (keys structs))
                   (inner-symbols struct-content))])
               structs))))

(defn layout-qualifiers [layout]
  (into {}
        (map (fn [[name location]]
               [name
                (str "layout(location = "
                     location
                     ")")])
             layout)))

(defn parsed-iglu->glsl [{:keys [version
                                 precision
                                 uniforms
                                 structs
                                 attributes
                                 varyings
                                 inputs
                                 outputs
                                 qualifiers
                                 layout
                                 main
                                 functions]
                          :as parsed-iglu}]
  (let [full-functions (cond-> functions
                         main (assoc 'main {{:in [] :out 'void}
                                            {:args [] :body main}}))
        full-qualifiers (merge qualifiers (layout-qualifiers layout))]
    (->> (into (cond-> []
                 version (conj (str "#version " version))
                 precision (into (mapv ->precision precision))
                 uniforms (into (mapv ->uniform uniforms))
                 attributes (into (mapv ->attribute attributes))
                 varyings (into (mapv ->varying varyings))
                 inputs (into (mapv (partial ->in full-qualifiers) inputs))
                 outputs (into (mapv (partial ->out full-qualifiers) outputs))
                 structs (into (mapv ->struct (sort-structs structs))))
               (vec (mapcat ->function (sort-fns full-functions))))
         (reduce (partial stringify 0) [])
         (join \newline))))
