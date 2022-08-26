(ns iglu.dev
  (:require [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s]))

(defn init []
  (st/instrument))
