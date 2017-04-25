(ns undercover.server
  (:require 
    [undercover.util :as util]
    [clojure.set :as set]
    [clojure.data.json :as json]
    [org.httpkit.server :as hk]
    [clojure.string :as str]))


;; The gargantuan server state
(defonce state (atom {
                      :rooms {}           ;; name -> set of ids
                      :chan-by-id {}      ;; id -> channel
                      :nick-by-id {}      ;; name -> id
                      :id-by-nick {}      ;; id -> name 
                    }))


(defn add-user-channel
  "Returns a new server state where the given uuid, nick and channels have been added. 
   Returns the same state value if not modified"
  [state uuid nick channel]
  (if (contains? (:nick-by-id state) nick)
    state
    (assoc state
      :chan-by-id (assoc (:chan-by-id state) uuid channel)
      :nick-by-id (assoc (:nick-by-id state) uuid nick)
      :id-by-nick (assoc (:id-by-nick state) nick uuid))))


(defn remove-channel
  "Returns a new server state where the channel (identified by uuid) has been removed.
   Returns the same state value if not modified"
  [state uuid]
  (if-let [nick (get (:nick-by-id state) uuid)]
    (assoc state
      :rooms (util/map-map-vals (:rooms state) (fn [x] (set/difference x #{uuid})))
      :chan-by-id (dissoc (:chan-by-id state) uuid)
      :nick-by-id (dissoc (:nick-by-id state) uuid)
      :id-by-nick (dissoc (:id-by-nick state) nick))
    state))

(defn get-nick
  [state uuid]
  (get-in state [:nick-by-id uuid]))

(defn has-nick?
  [state uuid]
  (boolean (get-nick state uuid)))

(defn get-room
  [state room]
  (get-in state [:rooms room]))

(defn get-room-chans
  "Returns a seq of channels, one for each user in the room"
  [state room]
  (let [uuids (get-room state room)
        chan-by-id (:chan-by-id state)]
    (remove nil? (map #(get chan-by-id %1) uuids))))

(defn add-user-to-room
  "Adds a user to a room"
  [state uuid room]
  (if-let [nick (get-nick state uuid)]
    (if-let [room (get-room state room)]
      (assoc-in state [:rooms room] (set/union room #{ uuid }))
      (assoc-in state [:rooms room] #{ uuid }))
    state))

(defn remove-user-from-room
  [state uuid room]
  (if-let [nick (get-nick state uuid)]
    (if-let [room (get-room state room)]
      (assoc-in state [:rooms room] (set/difference room #{ uuid }))
      state)
    state))

(defn add-user-channel! 
  [uuid nick channel]
  (swap! state add-user-channel uuid nick channel))

(defn add-user-to-room!
  [uuid room]
  (swap! state add-user-to-room uuid room))

(defn remove-user-from-room! 
  [uuid room]
  (swap! state remove-user-from-room uuid room))

(defn remove-channel!
  [uuid]
  (swap! state remove-channel uuid))

(defn handle-chan-closed! 
  "Handle the closing channel from http-kit"
  [uuid chan status] 
  (remove-channel! uuid))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Message Helpers
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def status-ok { :status :ok })

(defn res-kw 
  "Returns the proper response keyword from the request keyword"
  [req-kw]
  (keyword (str/replace (name req-kw) #"-req" "-res")))

(defn status-ko 
  [why] 
  { :status :ko 
    :why why })

(defn make-res-ok
  [req]
  { :id (:id req)
    :type (res-kw (:type req))
    :payload status-ok })

(defn make-res-ko
  [req-msg why]
  { :id (:id req-msg)
    :type (res-kw (:type req-msg))
    :payload (status-ko why) })

(defn make-res-ok-payload
  [req payload]
  (make-res-ok req))

(defn send! 
  "Sends clojure map as a json message to the destination channel"
  [chan msg] 
  (hk/send! chan (json/write-str msg)))

(defn broadcast!
  [chans msg]
  (map #(send! %1 msg) chans))

(defn make-msg
  "From raw json message to clojure map representing the message"
  [raw-json]
  (json/read-str raw-json 
    :key-fn keyword))

(defn make-user-room-feed
  [room nick action]
  { 
    :type :people-feed
    :payload { 
      :who nick
      :action action
      :room room
    }
  })

(defn make-user-leave-feed [room nick] (make-user-room-feed room nick :left-room))

(defn make-user-join-feed [room nick] (make-user-room-feed room nick :joined-room))

(defn make-room-chat-feed 
  [room nick chat-msg]
  {
    :type :room-chat
    :payload {
      :who nick 
      :msg chat-msg
      :room room
    }
  })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Message handlers
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; handle message dispatch on message type
(defmulti handle-msg! (fn [msg] (keyword (:type msg))))


(defmethod handle-msg! :login-req
  [uuid chan msg]
  (let [nick (get-in msg [:payload :nick])
        state (add-user-channel! uuid nick chan)
        req-ok? (= (get-in state [:nick-by-id nick]) uuid)]
    (if req-ok?
      (send! chan (make-res-ok msg))
      (send! chan (make-res-ko msg "Nickname already in use")))))


(defmethod handle-msg! :list-rooms-req
  [uuid chan msg]
  (send! chan (assoc (make-res-ok msg)
                  :rooms (:rooms @state))))


(defmethod handle-msg! :join-room-req
  [uuid chan msg]
  (let [room (get-in msg [:payload :room])
        state (add-user-to-room! uuid room)
        req-ok? (contains? (get-in state [:rooms room]) uuid)
        nick (get-nick state uuid)]
    (if req-ok?
      (do
        (send! chan (make-res-ok msg))  ;; res
        (broadcast! (get-room-chans state room) (make-user-join-feed room nick))) ;; notify people
      ;; req not ok
      (send! chan (make-res-ko msg "Could not join room")))))


(defmethod handle-msg! :leave-room-req
  [uuid chan msg]
  (let [room (get-in msg [:payload :room])
        state (remove-user-from-room! uuid room)]
    (send! chan (make-res-ok msg))
    (if-let [nick (get-nick state uuid)]
      (broadcast! (get-room-chans state room) (make-user-join-feed room nick)))))


(defmethod handle-msg! :say-req
  [uuid chan msg]
  (let [srv-state @state
        nick (get-nick srv-state uuid)
        chat-msg (get-in msg [:payload :msg])
        room-name (get-in msg [:payload :room])
        room (get-room state room-name)]
    (if (or (nil? nick) (not (contains? room nick)))
      (send! chan (make-res-ko msg "Not logged in yet or not part of the room"))
      (do 
        (send! chan (make-res-ok msg))
        (broadcast! room (make-room-chat-feed room nick chat-msg))))))


