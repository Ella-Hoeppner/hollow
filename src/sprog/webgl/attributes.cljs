(ns sprog.webgl.attributes
  (:require [sprog.util :as u]))

(defn set-boj-data! [gl {:keys [buffer usage]} data]
  (u/log buffer)
  (.bindBuffer gl
               gl.ARRAY_BUFFER
               buffer)
  (.bufferData gl
               gl.ARRAY_BUFFER
               data
               usage))

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

(defn ensure-attribute-present! [{:keys [gl program attributes-atom]} 
                                 attrib-name-str]
  (when (not (@attributes-atom attrib-name-str))
    (swap! attributes-atom 
           assoc
           attrib-name-str
           (.getAttribLocation gl
                               program
                               attrib-name-str))))

(defn set-sprog-attribute! [{:keys [gl attributes-atom] :as sprog}
                            attrib-name
                            {:keys [buffer
                                    num-components
                                    type
                                    normalized
                                    stride
                                    offset]}]
  (let [attrib-name-str (str attrib-name)]
    (ensure-attribute-present! sprog attrib-name-str)
    (let [location (@attributes-atom attrib-name)]
      (.bindBuffer gl gl.ARRAY_BUFFER buffer)
      (.enableVertexAttribArray gl location)
      (.vertexAttribPointer gl
                            location
                            num-components
                            type
                            normalized
                            stride
                            offset))))

(defn set-sprog-attributes! [sprog
                             attrib-boj-map]
  (doseq [[attrib-name boj] attrib-boj-map]
    (set-sprog-attribute! sprog attrib-name boj)))
