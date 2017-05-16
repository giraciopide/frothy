(ns undercover.protocol
  (:require 
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [clojure.string :as str]))

;;
;; Requests
;;

(s/def ::nick string?)

(s/def ::login-req-payload
  (s/keys :req-un [::nick]))

(s/def ::filter string?)

(s/def ::list-rooms-req-payload
  (s/keys :req-un []
          :opt-un [::filter]))

(s/def ::room string?)

(s/def ::join-room-req-payload
  (s/keys :req-un [::room]))

(s/def ::leave-room-req-payload
  (s/keys :req-un [::room]))

(s/def ::msg string?)

(s/def ::say-req-payload
  (s/keys :req-un [::room ::msg]))

(s/def ::to string?)

(s/def ::whisper-req-payload
  (s/keys :req-un [::to ::msg]))

;;
;; Responses
;;

(s/def ::response-status #{"ok" "ko"})

(s/def ::status ::response-status)
(s/def ::why string?)

(s/def ::response-payload
  (s/keys
    :req-un [ ::status ]
    :opt-un [ ::why ]))


(s/def ::rooms 
  (s/coll-of string?))

(s/def ::list-rooms-res-payload
  (s/keys :req-un [::status]
          :opt-un [::why ::rooms]))

(s/def ::people
  (s/coll-of string?))

(s/def ::join-room-res-payload
  (s/keys :req-un [::status]
          :opt-un [::why ::room ::people]))

;;
;; Feeds
;;

(s/def ::from string?)
(s/def ::whisper string?)

(s/def ::whisper-feed-payload
  (s/keys :req-un [::from ::whisper]))

(s/def ::who? string?)

(s/def ::room-chat-feed-payload
  (s/keys :req-un [::who ::room ::msg]))

(s/def ::userEvent
  #{ "left-room" "joined-room" })

(s/def ::people-feed-payload
  (s/keys :req-un [::who ::userEvent]
          :opt-un [::room]))

;;
;; Message structure
;;

(s/def ::type #{"ping-req" "login-req" "list-rooms-req" "join-room-req" "leave-room-req" "say-req" "whisper-req"
                "ping-res" "login-res" "list-rooms-res" "join-room-res" "leave-room-res" "say-res" "whisper-res"
                "room-chat-feed" "people-feed" "whisper-feed" })

(s/def ::payload 
  (s/or
    :response-payload           ::response-payload
    :login-req-payload          ::login-req-payload       
    :list-rooms-req-payload     ::list-rooms-req-payload
    :join-room-request-payload  ::join-room-request-payload
    :leave-room-req-payload     ::leave-room-req-payload
    :say-req-payload            ::say-req-payload
    :whisper-req-payload        ::whisper-req-payload
    :list-rooms-res-payload     ::list-rooms-res-payload
    :join-room-res-payload      ::join-room-res-payload
    :whisper-feed-payload       ::whisper-feed-payload
    :room-chat-feed-payload     ::room-chat-feed-payload
    :people-feed-payload        ::people-feed-payload))

(s/def ::id string?)

(s/def ::message 
  (s/keys :req-un [::type 
                   ::payload ]
          :opt-un [::id]))

(defn valid-msg? 
  "Validates a message against the protocol spec"
  [msg] 
  (s/valid? ::message msg))
