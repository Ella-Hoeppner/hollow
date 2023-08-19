(ns hollow.input.midi
  (:require [hollow.util :as u]
            [clojure.set :refer [union]]))

(defonce midi-callacks-atom (atom {}))

(defonce down-keys-atom (atom {}))

(def command-map
  {9 :note-down
   8 :note-up})

(defn on-midi-event [message]
  (let [[command-and-channel & data] (seq message.data)
        command (bit-shift-right command-and-channel 4)
        channel (bit-and command-and-channel 0xf)
        callbacks @midi-callacks-atom]
    (when-let [generic-callback (callbacks :generic)]
      (generic-callback {:command command
                         :channel channel
                         :data data}))
    (let [command-type (command-map command)]
      (when-let [command-callback (callbacks command-type)]
        (command-callback 
         (cond
           (= :note-down command-type)
           (assoc (zipmap [:note :velocity] data)
                  :channel
                  channel)

           (= :note-up command-type)
           {:note (first data)
            :channel channel})))
      (when (= command-type :note-down)
        (swap! down-keys-atom
               update
               channel
               (comp set conj)
               (first data)))
      (when (= command-type :note-up)
        (swap! down-keys-atom
               update
               channel
               disj
               (first data))))))

(defn add-generic-midi-callback
  "`callback`: a function that accepts a map containing the keys :command,
   :channel, and :data (the latter 2 midi data bytes)."
  [callback]
  (swap! midi-callacks-atom assoc :generic callback))

(defn add-note-down-callback
  "`callback`: a function that accepts a map containing the keys :channel,
   :note, and :velocity."
  [callback]
  (swap! midi-callacks-atom assoc :note-down callback))

(defn add-note-up-callback
  "`callback`: a function that accepts a map containing the keys :channel and
   :note."
  [callback]
  (swap! midi-callacks-atom assoc :note-up callback))

(defn down-notes
  ([channel]
   (set (@down-keys-atom channel)))
  ([]
   (apply union (vals @down-keys-atom))))

(.then (.requestMIDIAccess js/navigator)
       (fn [midi-access]
         (doseq [[_ input] midi-access.inputs]
           (set! input.onmidimessage on-midi-event))))
