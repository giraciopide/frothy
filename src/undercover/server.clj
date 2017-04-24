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

(defn make-msg
  "From raw json message to clojure map representing the message"
  [raw-json]
  (json/read-str raw-json 
    :key-fn keyword))

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
      (send! chan (make-res-ko msg "Nick already registered")))))


(defmethod handle-msg! :list-room-req
  [uuid chan msg]
  (send! chan (assoc make-res-ok
                  :rooms (:rooms @state))))


(defmethod handle-msg! :join-room-req
  [uuid chan msg]
  (let [room (get-in msg [:payload :room])]
    ))
