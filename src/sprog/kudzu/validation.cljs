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

(defn validate-type [t & [context]]
  (when-not (type-valid? t)
    (throw (str "KUDZU: Invalid type " t " in " context))))

(defn validate-name-type-pairs [name-type-pairs & [context]]
  (doseq [[name t] name-type-pairs]
    (validate-name name context)
    (validate-type t context)))

(defn validate-uniforms [uniforms]
  (validate-name-type-pairs uniforms "uniforms"))

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
                              (str "struct " name))))

(defn validate-precision [precision]
  (doseq [[type-name specifier] precision]
    (validate-type type-name "precision")
    (when-not ('#{highp mediump lowp "highp" "mediump" "lowp"} specifier)
      (throw (str "KUDZU: Invalid precision specifier for "
                  type-name
                  ": "
                  specifier)))))

(defn validate-in-outs [inputs outputs layout]
  (validate-name-type-pairs inputs "inputs")
  (validate-name-type-pairs inputs "outputs")
  (when (seq (difference
              (set (map clj-name->glsl (keys layout)))
              (union (set (map clj-name->glsl (keys inputs)))
                     (set (map clj-name->glsl (keys outputs))))))
    (throw (str "KUDZU: Unrecognized keys in layout "
                (seq (difference
                      (set (keys layout))
                      (union (set (keys inputs))
                             (set (keys outputs)))))))))

(defn validate-defines [defines])
