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
    (let [uuid (server/make-uuid)
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
    (let [uuid (server/make-uuid)
          state1 {:rooms {"room1" #{uuid}}}
          state2 (server/add-user-channel state1 uuid "nick" "channel")
          final-state (server/remove-channel state2 uuid)]
      (is (empty? (get-in final-state [:rooms "room1"])))
      (is (empty? (:chan-by-id final-state)))
      (is (empty? (:nick-by-id final-state)))
      (is (empty? (:id-by-nick final-state))))))

(deftest test-add-user-to-room1
  (testing "Testing that adding a user to a non-existent room works"
    (let [uuid (server/make-uuid)
          init-state {}
          state (server/add-user-channel init-state uuid "nick" "channel")
          final-state (server/add-user-to-room state uuid "room1")]
      (is (contains? (get-in final-state [:rooms "room1"]) uuid)))))

(deftest test-remove-user-from-room1
  (testing "Testing that adding a user to a non-existent room works"
    (let [uuid (server/make-uuid)
          init-state {}
          state (server/add-user-channel init-state uuid "nick" "channel")
          final-state (server/remove-user-from-room state uuid "room1")]
      (is (empty? (get-in final-state [:rooms "room1"]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers to perform real test with a real websocket client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce msg-id (atom 1))

(defn next-msg-id! [] (str (swap! msg-id inc)))

(defn recv-msg
  "Receive and decodes a message from a manifold stream"
  [q]
  (let [m @(m/take! q)]
    (println (str "recv: " m))
    (server/decode-msg m)))

(defn send-raw-msg
  [socket raw-str])

(defn send-msg
  "Encodes a message to a string and sends it to a ws/client"
  [socket msg]
  (ws/send-msg socket (server/encode-msg msg)))

(defn is-res-status
  [resp-msg status]
  (is (= status (get-in resp-msg [:payload :status]))))

(defn is-msg-of-type
  [msg msg-type]
  (is (= msg-type (:type msg))))

(defn is-res-ok
  ([resp-msg]
    (is-res-status resp-msg "ok"))
  ([resp-msg res-type]
    (is-msg-of-type resp-msg res-type) 
    (is-res-status resp-msg "ok")))

(defn is-res-ko
  [resp-msg]
  (is-res-status resp-msg "ko"))

(defn is-msg-type
  [message msg-type]
  (is (= msg-type (:type message))))

(defn make-server
  "Starts a server and returns a function that is used to shut it down"
  [host port]
  (core/start-server { :port port :ip host }))

(defn make-client
  [host port]
  (let [manifold-queue (m/stream)
        close-promise (promise)
        sock (ws/connect (str "ws://" host ":" (str port) "/chat")
                :on-receive #(m/put! manifold-queue %1)
                :on-close #(deliver close-promise (str %1 "/" %2)))]
    [sock manifold-queue close-promise]))

(defn make-client-and-server
  [host port]
  (let [stop-server (make-server host port)
        [sock q close-promise] (make-client host port)]
    [sock q close-promise stop-server]))

(defn is-login-ok
  [sock q nick]
  (send-msg sock { :id (next-msg-id!) 
                   :type :login-req 
                   :payload { :nick nick }})
  (is-res-ok (recv-msg q) "login-res"))


(defn sleep-briefly
  ([] (java.lang.Thread/sleep 200))
  ([millis] (java.lang.Thread/sleep millis)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Real tests regarding setting up a server and connecting to it with a websocket client.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def test-host "127.0.0.1")

(def test-port 9876)

(deftest test-malformed-message-causes-disconnect
  (testing "Testing the upon a malformed message the server will disconnect the client"
    (let [[sock q close-promise stop-server] (make-client-and-server test-host test-port)]
      (send-msg sock { :whatever "whatever "}) ;; this is a malformed message, we don't even have an :id field.
      (sleep-briefly)
      (is (= true (realized? close-promise)))
      (ws/close sock)
      (stop-server))))

(deftest test-malformed-message-with-id-gets-responded
  (testing "Testing the upon a malformed message the server will disconnect the client"
    (let [[sock q close-promise stop-server] (make-client-and-server test-host test-port)]
      (send-msg sock { :id "id" :whatever "whatever "}) ;; this is a malformed message, we don't even have an :id field.
      (is-res-ko (recv-msg q))
      (sleep-briefly)
      (is (= false (realized? close-promise)))
      (ws/close sock)
      (stop-server))))

(deftest test-login-session-1
  (testing "Testing that login in works works"
    (let [[sock q close-promise stop-server] (make-client-and-server test-host test-port)]
      (send-msg sock { :id (next-msg-id!)
                       :type :login-req
                       :payload { :nick "JSBach" }})
      (is-res-ok (recv-msg q))
      (ws/close sock)
      (stop-server))))

;; TODO complete this test
(deftest test-join-and-leave-room
  (testing "Testing that login in works works"
    (let [stop-server (make-server test-host test-port)
          [sock1 q1 close-promise] (make-client test-host test-port) 
          [sock2 q2 close-promise] (make-client test-host test-port)]
      (is-login-ok sock1 q1 "JSBach")
      (is-login-ok sock2 q2 "AVivaldi")
      (ws/close sock1)
      (ws/close sock2)
      (stop-server))))



