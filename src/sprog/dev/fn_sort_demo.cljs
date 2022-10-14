(ns sprog.dev.fn-sort-demo
  (:require [sprog.util :as u]
            [sprog.webgl.canvas :refer [create-gl-canvas
                                        maximize-gl-canvas]]
            [sprog.webgl.shaders :refer [create-purefrag-sprog
                                         run-purefrag-sprog]]
            [sprog.webgl.framebuffers :refer [target-screen!]]
            [sprog.iglu.core :refer [iglu->glsl
                                     merge-chunks]]
            [clojure.walk :refer [postwalk-replace]]))

(def fn-count 50)

(defonce gl-atom (atom nil))
(defonce sprog-atom (atom nil))

(def frag-source
  (reduce merge-chunks
          (postwalk-replace
           {:final-fn-name (symbol (str "f" fn-count))
            :fn-count-f (.toFixed fn-count 1)}
           '{:version "300 es"
             :precision {float highp}
             :uniforms {size vec2}
             :outputs {fragColor vec4}
             :signatures {main ([] void)}
             :functions {main
                         ([]
                          (= fragColor (vec4 (:final-fn-name "0.0")
                                             0
                                             0
                                             1)))}})
          (map (fn [i]
                 (postwalk-replace
                  {:fn-name (symbol (str "f" (inc i)))
                   :prev-fn-name (symbol (str "f" i))}
                  (if (zero? i)
                    '{:signatures {:fn-name ([float] float)}
                      :functions {:fn-name ([x] (+ x "0.01"))}}
                    '{:signatures {:fn-name ([float] float)}
                      :functions {:fn-name
                                  ([x] (+ (:prev-fn-name x) "0.01"))}})))
               (range fn-count))))

(defn update-page! []
  (let [gl @gl-atom
        resolution [gl.canvas.width gl.canvas.height]]
    (maximize-gl-canvas gl)
    (target-screen! gl)
    (run-purefrag-sprog @sprog-atom
                        resolution
                        {:floats {"size" resolution}})
    (js/requestAnimationFrame update-page!)))

(defn init []
  (let [gl (create-gl-canvas)]
    (reset! gl-atom gl)
    (reset! sprog-atom (create-purefrag-sprog
                        gl
                        (iglu->glsl frag-source))))
  (update-page!))
