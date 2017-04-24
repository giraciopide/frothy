(ns undercover.server-test
  (:require
    [clojure.test :refer :all]
    [undercover.core :as core]
    [undercover.server :as server]
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
