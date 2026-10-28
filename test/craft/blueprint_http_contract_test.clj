(ns craft.blueprint-http-contract-test
  "Contract tests for Blueprint 3D Engine, volume slicing, and all general HTTP endpoints."
  (:require [clojure.test :refer [deftest is testing]]
            [craft.fixtures :as fix]
            [craft.schemas :as schemas]
            [craft.test-helper :as th :refer [with-sandbox-server *sandbox*]]))

(deftest test-telemetry-and-waypoints-contracts
  (with-sandbox-server
    (testing "GET /api/telemetry returns full telemetry conforming to Malli schema"
      (let [{:keys [status body]} (th/http-get "/api/telemetry")]
        (is (= 200 status))
        (is (schemas/valid? schemas/TelemetryResponse body))
        (is (vector? (:players body)))
        (is (= 2 (count (:players body))))
        (is (every? #(boolean? (:online %)) (:players body)))))

    (testing "GET and POST /api/waypoints"
      (let [{:keys [status body]} (th/http-get "/api/waypoints")]
        (is (= 200 status))
        (is (seq body)))
      (let [{:keys [status body]} (th/http-post "/api/waypoints"
                                               {:name "Mina de Ferro" :x -200 :y 30 :z -100 :icon "⛏️"})]
        (is (= 200 status))
        (is (true? (:ok body)))))))

(deftest test-run-command-chaining-and-substitution
  (with-sandbox-server
    (testing "POST /api/run handles multiple commands separated by && or ; and replaces @s"
      (let [{:keys [status body]} (th/http-post "/api/run"
                                               {:cmd "time set day && effect give @s night_vision infinite ; playsound minecraft:block.bell.use master @s"})]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"time set day" %) cmds))
          ;; @s was substituted by 'pace'
          (is (some #(re-find #"effect give pace night_vision" %) cmds))
          (is (some #(re-find #"playsound .* pace" %) cmds)))))))

(deftest test-blueprint-backup-paste-and-undo-slicing
  (with-sandbox-server
    (testing "Blueprint 3D engine backs up, pastes with offset and slices undo vertically"
      ;; 1. Backup blueprint
      (let [{:keys [status body]} (th/http-post "/api/blueprint/backup")]
        (is (= 200 status))
        (is (schemas/valid? schemas/BlueprintMetaResponse body))
        (is (true? (:ok body))))

      ;; 2. Paste blueprint at player location (-146, 107, -85)
      ;; Expected offset: (-146 - 18 = -164, 107 - 8 = 99, -85 - 27 = -112)
      (let [{:keys [status body]} (th/http-post "/api/blueprint/paste?player=pace")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"place template blueprints:base_principal -164 99 -112" %) cmds))))

      ;; 3. Undo paste - Must split vertically at mid_y so each /fill has <= 32768 blocks!
      (let [{:keys [status body]} (th/http-post "/api/blueprint/undo-paste")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))
              fill-cmds (filter #(re-find #"fill .* air replace" %) cmds)]
          ;; Exactly two sliced /fill commands
          (is (= 2 (count fill-cmds)))
          (is (some #(re-find #"fill -164 109 -112 -127 118 -65 air replace" %) fill-cmds))
          (is (some #(re-find #"fill -164 99 -112 -127 108 -65 air replace" %) fill-cmds)))))))

(deftest test-map-redirect-and-skins
  (with-sandbox-server
    (testing "GET /map redirects 301 to /map/"
      (let [{:keys [status headers]} (th/http-get "/map")]
        (is (= 301 status))
        (is (= ["/map/"] (get headers "location")))))

    (testing "GET /skins/pace.png serves image with correct content-type"
      (let [{:keys [status headers]} (th/http-get "/skins/pace.png")]
        (is (= 200 status))
        (is (= ["image/png"] (get headers "content-type")))))))
