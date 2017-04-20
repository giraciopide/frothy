(ns undercover.server
  (:require 
    [clojure.set :as set]
    [clojure.string :as str]))

(defn make-uuid [] (str (java.util.UUID/randomUUID)))

;; the fucking gargantuan server state
(defonce state (atom {
                      :rooms {}           ;; name -> set of id
                      :chan-by-id {}      ;; id -> channel
                      :nick-by-id {}      ;; name -> id
                      :id-by-nick {}      ;; id -> name 
                    }))

(defn add-user-channel
  "Returns a new server state where the given uuid, nick and channels have been added"
  [state uuid nick channel]
  (assoc state
    :chan-by-id (assoc (:chan-by-id state) uuid channel)
    :nick-by-id (assoc (:nick-by-id state) nick uuid)
    :id-by-nick (assoc (:id-by-nick state) uuid nick)))

(defn remove-channel
  "Returns a new server state where the channel has been removed"
  [state uuid]
  (if-let [nick (get (:nick-by-id state) uuid)]
    (assoc state
      :chans-by-id (dissoc (:chan-by-id state) uuid)
      :nick-by-id (dissoc (:nick-by-id state) nick)
      :id-by-nick (dissoc (:id-by-nick state) uuid))
    (state)))

(defn add-user-channel! 
  [uuid nick channel]
  (let [uiid (make-uuid)]
    (swap! state add-user-channel uuid nick channel)))

(defn remove-channel!
  [uuid]
  (swap! state remove-channel uuid))