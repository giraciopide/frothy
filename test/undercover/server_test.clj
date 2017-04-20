(ns undercover.server
  (:require 
    [undercover.core :as core]
    [undercover.server :as server]
    [clojure.string :as str]))

(deftest test-add-user-channel
  (testing "Testing that adding a user channel works"
    (let [uuid (core/make-uuid)
          initial-state {}
          state (server/add-user-channel initial-state uuid "nick" "channel")]
      (is (= 1 (count (:chan-by-id state))))
      (is (= 1 (count (:nick-by-id state))))
      (is (= 1 (count (:id-by-nick state)))))))

(deftest test-remove-user-channel
  (testing "Testing that removing a user channel works"
    (let [uuid (core/make-uuid)
          initial-state (server/add-user-channel {} uuid "nick" "channel")
          state (server/remove-channel initial-state uuid)]
      (is (empty? (:chan-by-id state)))
      (is (empty? (:nick-by-id state)))
      (is (empty? (:id-by-nick state))))))
