(ns craft.rail-spiral-contract-test
  "Contract tests for the circular rail spiral (minecart helix from ground to a target Y)."
  (:require [clojure.test :refer [deftest is testing]]
            [craft.fixtures :as fix]
            [craft.schemas :as schemas]
            [craft.test-helper :as th :refer [with-sandbox-server *sandbox*]]))

(deftest test-rail-spiral-build-contract
  (with-sandbox-server
    (testing "Rail spiral rises from ground to target Y with supported, walled and lit track"
      (let [{:keys [status body]} (th/http-post "/api/build/rail-spiral?player=pace&x=-47&y=65&z=263&radius=7&height=100&turns=7")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (is (re-find #"Espiral" (:message body)))

        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          ;; Track: powered rails provide the climb, plain rail fills the gaps
          (is (some #(re-find #"powered_rail" %) cmds))
          (is (some #(re-find #"\brail(?:\[| )" %) cmds))
          ;; Support under the track, containment walls on both sides
          (is (some #(re-find #"stone_brick_wall" %) cmds))
          ;; Lighting against mobs
          (is (some #(re-find #"lantern" %) cmds))
          ;; Arrival station at the top (Y=100) with a stopping detector rail
          (is (some #(re-find #"detector_rail" %) cmds))
          (is (some #(re-find #"setblock -?\d+ 100 " %) cmds))
          ;; Rails are emitted last: a rail set before its solid base is dropped
          ;; by the server on the next tick.
          (let [idx (fn [pred] (first (keep-indexed (fn [i c] (when (pred c) i)) cmds)))
                support (idx #(re-find #"stone_bricks" %))
                rail (idx #(re-find #"\brail(?:\[| )" %))]
            (is (and support rail (< support rail)))))))

    (testing "Undo clears the spiral envelope and restores the ground"
      (let [{:keys [status body]} (th/http-post "/api/build/undo-rail-spiral")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"fill .* air replace" %) cmds))
          (is (some #(re-find #"fill .* grass_block replace" %) cmds)))))))

(deftest test-rail-spiral-height-and-radius-are-honored
  (with-sandbox-server
    (testing "Requested radius bounds the horizontal extent and height bounds the vertical extent"
      (let [{:keys [status body]} (th/http-post "/api/build/rail-spiral?player=pace&x=0&y=64&z=0&radius=5&height=80&turns=4")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))
              coords (keep (fn [c]
                             (when-let [m (re-find #"setblock (-?\d+) (\d+) (-?\d+) (\S+)" c)]
                               (let [[_ x y z blk] m]
                                 (when (re-find #"rail" blk)
                                   [(Integer/parseInt x) (Integer/parseInt y) (Integer/parseInt z)]))))
                           cmds)]
          (is (seq coords) "espera-se trilhos gerados")
          ;; Every rail cell stays within the requested radius (grid walk may
          ;; bulge the corner by at most one block).
          (is (every? (fn [[x _ z]] (<= (+ (* x x) (* z z)) (* 6 6))) coords))
          ;; Vertical extent never exceeds the requested height
          (is (every? (fn [[_ y _]] (<= y 80)) coords)))))))
