(ns sprog.webgl.attributes
  (:require [sprog.kudzu.tools :refer [clj-name->glsl]]))

(defn set-boj-data! [gl {:keys [buffer usage elements?] :as boj} data] 
  (let [binding-point (if elements? gl.ELEMENT_ARRAY_BUFFER gl.ARRAY_BUFFER)]
    (.bindBuffer gl
                 binding-point
                 buffer)
    (.bufferData gl
                 binding-point
                 data
                 usage))
  boj)

(defn create-boj! [gl num-components & [{:keys [type
                                                normalized
                                                elements?
                                                stride
                                                offset
                                                usage
                                                initial-data]
                                         :or {type gl.FLOAT
                                              normalized false
                                              elements? false
                                              stride 0
                                              offset 0
                                              usage gl.STATIC_DRAW}}]]
  (let [boj {:buffer (.createBuffer gl)
             :num-components num-components
             :type type
             :normalized normalized
             :elements? elements?
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
