(ns sprog.kudzu.validation
  (:require [sprog.kudzu.tools :refer [clj-name->glsl]]
            [clojure.set :refer [union
                                 difference]]))

(defn validate-kudzu-keys [shader]
  (when-let [unrecognized-keys
             (seq (difference (set (keys shader))
                              #{:precision
                                :uniforms
                                :layout
                                :qualifiers
                                :inputs
                                :outputs
                                :structs
                                :defines
                                :functions
                                :global
                                :constants
                                :main}))]
    (throw (str "KUDZU: Unrecognized keys in kudzu map "
                unrecognized-keys))))

(defn name-valid? [name]
  (or (symbol? name) (string? name)))

(defn validate-name [name & [context]]
  (when-not (name-valid? name)
    (throw (str "KUDZU: Invalid name " name " in " context))))

(defn int-valid? [int-str]
  (and (str int-str)
       (= int-str (str (js/parseInt int-str)))))

(defn type-valid? [t]
  (or (symbol? t)
      (string? t)
      (and (vector? t)
           (= (count t) 2)
           (type-valid? (first t))
           (int-valid? (second t)))))

(defn validate-type [t context]
  (when-not (type-valid? t)
    (throw (str "KUDZU: Invalid type " t context))))

(defn validate-name-type-pairs [name-type-pairs & [context]]
  (doseq [[name t] name-type-pairs]
    (validate-name name context)
    (validate-type t (str " for " name context))))

(defn validate-uniforms [uniforms]
  (validate-name-type-pairs uniforms " in uniforms"))

(defn validate-structs [structs]
  (doseq [[name struct-definition] structs]
    (validate-name name "structs")
    (when-not (and (vector? struct-definition)
                   (even? (count struct-definition)))
      (throw (str "KUDZU: Invalid struct definition for "
                  name
                  ": "
                  struct-definition)))
    (validate-name-type-pairs (partition 2 struct-definition)
                              (str "in struct " name))))

(defn validate-precision [precision]
  (doseq [[type-name specifier] precision]
    (validate-type type-name " in precision")
    (when-not ('#{highp mediump lowp "highp" "mediump" "lowp"} specifier)
      (throw (str "KUDZU: Invalid precision specifier for "
                  type-name
                  ": "
                  specifier)))))

(defn validate-in-outs [inputs outputs layout qualifiers]
  (validate-name-type-pairs inputs " in inputs")
  (validate-name-type-pairs inputs " in outputs")
  (doseq [[modifier-map modifier-name]
          [[qualifiers "qualifiers"]
           [layout "layout"]]]
    (when (seq (difference
                (set (map clj-name->glsl (keys modifier-map)))
                (union (set (map clj-name->glsl (keys inputs)))
                       (set (map clj-name->glsl (keys outputs))))))
      (throw (str "KUDZU: Unrecognized keys in " modifier-name " "
                  (seq (difference
                        (set (keys modifier-map))
                        (union (set (keys inputs))
                               (set (keys outputs)))))))))
  (doseq [[qualifier-name qualifier] qualifiers]
    (when-not (name-valid? qualifier)
      (throw (str "KUDZU: Invalid qualifier for "
                  qualifier-name
                  ": "
                  qualifier)))))

(defn expression-valid? [expression]
  (or (string? expression)
      (symbol? expression)
      (number? expression)
      (keyword? expression)
      (and (or (seq? expression)
               (and (vector? expression)
                    (#{2 3} (count expression))))
           (reduce #(and %1 %2)
                   (map expression-valid? expression)))))

(defn validate-defines [defines]
  (doseq [[pattern replacement] defines]
    (when-not (expression-valid? pattern)
      (throw (str "KUDZU: Invalid pattern in define: " pattern)))
    (when-not (expression-valid? replacement)
      (throw (str "KUDZU: Invalid replacement in define: " replacement)))))

(defn validate-function-body [[return-type args-and-types & statements]
                              fn-name
                              & [multi-body]]
  (when-not (type-valid? return-type)
    (throw (str "KUDZU: Invalid return type for function "
                fn-name
                ": "
                return-type)))
  (validate-name-type-pairs (partition 2 args-and-types)
                            (str " in function " fn-name))
  (when-not (expression-valid? statements)
    (throw (str "KUDZU: Invalid function body for function"
                fn-name
                (when multi-body (str "(signature: "
                                      return-type
                                      " "
                                      args-and-types
                                      ")"))
                "\n"
                statements))))

(defn validate-functions [functions]
  (doseq [[fn-name fn-body] functions]
    (when-not (name-valid? fn-name)
      (throw (str "KUDZU: Invalid function name: " fn-name)))
    (cond
      (seq? fn-body)
      (validate-function-body fn-body fn-name)

      (vector? fn-body)
      (doseq [sub-body fn-body]
        (validate-function-body sub-body fn-name true))

      :else (throw (str "KUDZU: Invalid function body for " fn-name)))))
