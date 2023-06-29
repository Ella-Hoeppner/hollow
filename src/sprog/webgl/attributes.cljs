(ns sprog.webgl.attributes
  (:require [sprog.kudzu.compiler :refer [clj-name->glsl]]))

(defn set-boj-data! [gl {:keys [buffer usage] :as boj} data] 
  (.bindBuffer gl
               gl.ARRAY_BUFFER
               buffer)
  (.bufferData gl
               gl.ARRAY_BUFFER
               data
               usage)
  boj)

(defn create-boj! [gl num-components & [{:keys [type
                                                normalized
                                                stride
                                                offset
                                                usage
                                                initial-data]
                                         :or {type gl.FLOAT
                                              normalized false
                                              stride 0
                                              offset 0
                                              usage gl.STATIC_DRAW}}]]
  (let [boj {:buffer (.createBuffer gl)
             :num-components num-components
             :type type
             :normalized normalized
             :stride stride
             :offset offset
             :usage usage}]
    (when initial-data
      (set-boj-data! gl boj initial-data))
    boj))

(defn ensure-attribute-present! [gl
                                 {:keys [program attribute-locations-atom]} 
                                 attrib-name-str]
  (when (not (@attribute-locations-atom attrib-name-str))
    (swap! attribute-locations-atom 
           assoc
           attrib-name-str
           (.getAttribLocation gl
                               program
                               attrib-name-str))))

(defn set-sprog-attribute! [gl
                            {:keys [attribute-locations-atom] :as sprog}
                            attrib-name
                            {:keys [buffer
                                    num-components
                                    type
                                    normalized
                                    stride
                                    offset]}]
  (let [attrib-name-str (clj-name->glsl attrib-name)]
    (ensure-attribute-present! gl sprog attrib-name-str)
    (let [location (@attribute-locations-atom attrib-name)]
      (.bindBuffer gl gl.ARRAY_BUFFER buffer)
      (.enableVertexAttribArray gl location)
      (.vertexAttribPointer gl
                            location
                            num-components
                            type
                            normalized
                            stride
                            offset))))

(defn set-sprog-attributes! [gl
                             sprog
                             attrib-boj-map]
  (doseq [[attrib-name boj] attrib-boj-map]
    (set-sprog-attribute! gl sprog attrib-name boj)))
