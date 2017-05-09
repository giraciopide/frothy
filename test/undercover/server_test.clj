(ns undercover.server-test
  (:require
    [clojure.test :refer :all]
    [undercover.core :as core]
    [undercover.server :as server]
    [gniazdo.core :as ws]
    [manifold.stream :as m]
    [clojure.string :as str]))

(deftest test-add-user-channel
  (testing "Testing that adding a user channel works"
    (let [uuid (core/make-uuid)
          initial-state {}
          state (server/add-user-channel initial-state uuid "nick" "channel")]
      (is (= 1 (count (:chan-by-id state))))
      (is (= uuid (first (keys (:chan-by-id state)))))
      (is (= 1 (count (:nick-by-id state))))
      (is (= uuid (first (keys (:nick-by-id state)))))
      (is (= 1 (count (:id-by-nick state))))
      (is (= "nick" (first (keys (:id-by-nick state))))))))


(deftest test-remove-user-channel
  (testing "Testing that removing a user channel works"
    (let [uuid (core/make-uuid)
          state1 {:rooms {"room1" #{uuid}}}
          state2 (server/add-user-channel state1 uuid "nick" "channel")
          final-state (server/remove-channel state2 uuid)]
      (is (empty? (get-in final-state [:rooms "room1"])))
      (is (empty? (:chan-by-id final-state)))
      (is (empty? (:nick-by-id final-state)))
      (is (empty? (:id-by-nick final-state))))))

(deftest test-add-user-to-room1
  (testing "Testing that adding a user to a non-existent room works"
    (let [uuid (core/make-uuid)
          init-state {}
          state (server/add-user-channel init-state uuid "nick" "channel")
          final-state (server/add-user-to-room state uuid "room1")]
      (is (contains? (get-in final-state [:rooms "room1"]) uuid)))))

(deftest test-remove-user-from-room1
  (testing "Testing that adding a user to a non-existent room works"
    (let [uuid (core/make-uuid)
          init-state {}
          state (server/add-user-channel init-state uuid "nick" "channel")
          final-state (server/remove-user-from-room state uuid "room1")]
      (is (empty? (get-in final-state [:rooms "room1"]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Real tests regarding setting up a server and connecting to it with a websocket client.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recv-msg
  "Receive and decodes a message from a manifold stream"
  [q]
  (server/decode-msg @(m/take! q)))

(defn send-msg
  "Encodes a message to a string and sends it to a ws/client"
  [socket msg]
  (ws/send-msg socket (server/encode-msg msg)))

(defn assert-res-ok
  [response-msg]
  (is (= "ok" (get-in response-msg [:payload :status]))))

(deftest test-chat-session-1
  (testing "Testing that login in works works"
    (let [shutdown-server (core/start-server { :port 9876 :ip "127.0.0.1" })
          q (m/stream)
          sock (ws/connect "ws://127.0.0.1:9876/chat"
                  :on-receive #(m/put! q %1))] ;; on receive put onto the stream.
      (send-msg sock { :id "1" 
                             :type :login-req
                             :payload { :nick "JSBach" }})
      (assert-res-ok (recv-msg q))
      (ws/close sock)
      (shutdown-server))))

