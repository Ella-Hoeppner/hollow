(ns sprog.wort.core
  (:require [sprog.util :as u]
            [clojure.string
             :refer [escape replace join]
             :rename {replace string-replace}]))

(defn symbol->wgsl [s]
  (escape (str s)
          {"-" "_"}))

(defn typed-name->wgsl [[name type]]
  (str name " : " type))

(def infix-operators
  #{'+ '- '* '/ '%})

(defn form->wgsl [form]
  (cond
    (or (number? form)
        (symbol? form))
    (symbol->wgsl form)

    :else (let [[f & args] form]
            (cond
              (and (= f '-) (= (count args) 1))
              (str "-" (form->wgsl (first args)))

              (infix-operators f)
              (str "("
                   (join (str " " f " ") (map form->wgsl
                                              args))
                   ")")

              :else (str f
                         "("
                         (join ", "
                               (map form->wgsl args))
                         ")")))))

(defn body->wgsl [body returns?]
  (if (empty? body)
    ""
    (let [forms (map form->wgsl body)]
      (str (join ";\n"
                 (if returns?
                   (concat (butlast forms)
                           (list (str "return " (last forms))))
                   forms))
           ";"))))

(defn indent [block]
  (str "  " (string-replace block "\n" "\n  ")))

(defn tag->wgsl [tag]
  (str "@"
       (symbol->wgsl (first tag))
       (when (> (count tag) 1)
         (str "("
              (join ", " (map symbol->wgsl (rest tag)))
              ")"))))

(defn function->wgsl [name fn-spec]
  (let [signature (first fn-spec)
        tags (filter vector? (rest fn-spec))
        body (filter (comp not vector?) fn-spec)
        return-type-index (some #(when (symbol? (signature %))
                                   %)
                                (range (count signature)))]
    (str (when (seq tags)
           (str (apply str (join " " (map tag->wgsl tags)))
                "\n"))
         "fn "
         (symbol->wgsl name)
         "("
         (apply str
                (join ", "
                      (map (fn [input]
                             (if (= (count input) 2)
                               (typed-name->wgsl input)
                               (str (join " "
                                          (map tag->wgsl (drop 2 input)))
                                    " "
                                    (typed-name->wgsl input))))
                           (if return-type-index
                             (take return-type-index signature)
                             signature))))
         ") "
         (when return-type-index
           (str "-> "
                (let [tags (drop (inc return-type-index) signature)]
                  (when (seq tags)
                    (str (join " " (map tag->wgsl tags)) " ")))
                (symbol->wgsl (signature return-type-index))
                " "))
         "{\n"
         (indent (body->wgsl body (boolean return-type-index)))
         "\n}")))

(defn wort->wgsl [wort-shader]
  (apply str
         (apply concat
                (butlast
                 (interleave
                  (map (fn [[fn-name fn-spec]]
                         (str (function->wgsl fn-name fn-spec)
                              "\n"))
                       (:functions wort-shader))
                  (repeat "\n"))))))

(comment
  (print
   (wort->wgsl
    '{:functions
      {negate ([[v vec3f] vec3f]
               (- v))
       vertex_entry_point ([[idx u32 [builtin vertex-index]]
                            vec4f
                            [builtin position]]
                           [vertex]
                           (vec4f (f32 (- (% idx 2) 1))
                                  (f32 (- (/ idx 2) 1))
                                  1
                                  1))
       fragment-main ([[pos vec4f [builtin position]]
                       vec4f
                       [location 0]]
                      [fragment]
                      (vec4 0 1 0 1))
       another_compute_entry_point ([]
                                    [compute]
                                    [workgroup_size 16])}}))
  )
