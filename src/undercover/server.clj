(ns undercover.server
  (:require 
    [undercover.util :as util]
    [clojure.set :as set]
    [clojure.string :as str]))


;; (defn make-uuid [] (str (java.util.UUID/randomUUID)))


;; The gargantuan server state
(defonce state (atom {
                      :rooms {}           ;; name -> set of ids
                      :chan-by-id {}      ;; id -> channel
                      :nick-by-id {}      ;; name -> id
                      :id-by-nick {}      ;; id -> name 
                    }))


(defn add-user-channel
  "Returns a new server state where the given uuid, nick and channels have been added"
  [state uuid nick channel]
  (assoc state
    :chan-by-id (assoc (:chan-by-id state) uuid channel)
    :nick-by-id (assoc (:nick-by-id state) uuid nick)
    :id-by-nick (assoc (:id-by-nick state) nick uuid)))


(defn remove-channel
  "Returns a new server state where the channel (identified by uuid) has been removed"
  [state uuid]
  (let [nick (get (:nick-by-id state) uuid)]
    (assoc state
      :rooms (util/map-map-vals (:rooms state) (fn [x] (set/difference x #{uuid})))
      :chan-by-id (dissoc (:chan-by-id state) uuid)
      :nick-by-id (dissoc (:nick-by-id state) uuid)
      :id-by-nick (dissoc (:id-by-nick state) nick))))
  

(defn add-user-channel! 
  [uuid nick channel]
  (swap! state add-user-channel uuid nick channel))


(defn remove-channel!
  [uuid]
  (swap! state remove-channel uuid))