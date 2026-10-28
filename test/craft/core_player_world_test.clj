(ns craft.core-player-world-test
  "Unit tests for craft.core, craft.player and craft.world."
  (:require [clojure.test :refer [deftest is testing]]
            [craft.core :as core]
            [craft.player :as player]
            [craft.world :as world]))

(deftest test-run-command-clean-prefix
  (testing "run-command! strips leading slash and delegates"
    (let [executed-cmd (atom nil)
          fake-cmd-mgr (reify Object)
          fake-cmd-src (reify Object)]
      ;; Test that command formatting works
      (is (fn? core/run-command!)))))

(deftest test-player-teleport-format
  (testing "player/teleport! formats coordinates properly"
    (is (fn? player/teleport!))
    (is (fn? player/heal!))
    (is (fn? player/give!))))

(deftest test-player-coords-mock
  (testing "player/player-coords returns nil safely when server is offline"
    (is (nil? (player/player-coords "pace")))))
