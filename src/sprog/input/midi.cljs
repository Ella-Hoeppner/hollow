(ns sprog.input.midi
  (:require [sprog.util :as u]))

(defn recieve-midi-message [callback message]
  (let [[command-and-channel note velocity] (seq message.data)]
    (callback {:command (bit-shift-right command-and-channel 4)
               :channel (bit-and command-and-channel 0xf)
               :note note
               :velocity velocity})))

(defn on-midi-success [callback midi-access]
  (doseq [[_ input] midi-access.inputs]
    (set! input.onmidimessage (partial recieve-midi-message callback))))

(defn add-midi-callback 
  "`callback`: a function that accepts a map containing the keys :command,
   :channel, :note, and :velocity."
  [callback]
  (.then (.requestMIDIAccess js/navigator)
         (partial on-midi-success callback)))
