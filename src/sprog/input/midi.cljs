(ns sprog.input.midi
  (:require [sprog.util :as u]))

; callback should be fn that accepts 4 element vec
; a parsed midi message, command, channel,
; note, and velocity in that order
(defn get-midi-message [callback message]
  (let [data (into [] (js->clj message.data))
        command (bit-shift-right (first data) 4)
        channel (bit-and (first data) 0xf)
        note (second data)
        velocity (last data)]
    (callback [command
               channel
               note
               velocity])))

(defn on-midi-failure []
  (js/console.log "cannot enable MIDI!"))

(defn on-midi-success  [midi-access callback]
  (js/console.log "MIDI enabled")
  
  (->   midi-access
        .-inputs
        (.forEach (fn [input]
                    (set! input.onmidimessage
                          (partial get-midi-message callback))))))

(defn initialize-midi [callback]
  (-> (.requestMIDIAccess js/navigator)
      (.then #(on-midi-success % callback))
      (.catch #(on-midi-failure))))