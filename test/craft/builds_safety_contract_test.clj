(ns craft.builds-safety-contract-test
  "Contract and generative property tests for builds, rotation, undo and critical safety locks."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [craft.fixtures :as fix]
            [craft.schemas :as schemas]
            [craft.test-helper :as th :refer [with-sandbox-server *sandbox*]]))

(deftest test-custom-house-rotation-and-undo
  (with-sandbox-server
    (testing "Custom house rotates correctly according to cardinal direction and undo restores terrain"
      ;; Player pace is at yaw 180 (looking North) in default fixture
      (let [{:keys [status body]} (th/http-post "/api/build/custom-house?player=pace&wood=cherry&w=9&l=11&h=4")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (is (re-find #"Norte" (:message body)))
        
        ;; Verify North door placement
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"setblock .* cherry_door\[half=lower,facing=north\]" %) cmds))
          (is (some #(re-find #"stripped_cherry_log" %) cmds))))

      ;; Undo custom house
      (let [{:keys [status body]} (th/http-post "/api/build/undo-custom-house")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"fill .* grass_block replace" %) cmds)))))))

(deftest test-custom-house-bamboo-and-manual-dir
  (with-sandbox-server
    (testing "Custom house supports bamboo special naming and Portuguese direction override"
      (let [{:keys [status body]} (th/http-post "/api/build/custom-house?player=pace&wood=bamboo&w=9&l=9&h=4&dir=leste")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (is (re-find #"Leste" (:message body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"stripped_bamboo_block" %) cmds))
          (is (some #(re-find #"bamboo_door\[half=lower,facing=east\]" %) cmds)))))))

(deftest test-nuke-critical-safety-lock
  (with-sandbox-server
    (testing "CRITICAL: Nuke is strictly locked when player is within 40m of main base (-146, 107, -85)"
      ;; In fixture, player pace is at (-146, 107, -85) => 0m distance from base
      (let [{:keys [status body]} (th/http-post "/api/nuke/launch?player=pace")]
        (is (= 500 status))
        (is (false? (:ok body)))
        (is (re-find #"PERIGO CRÍTICO" (:message body)))
        
        ;; Assert ZERO destructive commands recorded
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (not-any? #(re-find #"summon lightning_bolt" %) cmds))
          (is (not-any? #(re-find #"fill .* magma_block" %) cmds))
          (is (not-any? #(re-find #"summon creeper" %) cmds)))))))

(deftest test-nuke-authorization-when-safe-distance
  (with-sandbox-server
    (testing "Nuke strike proceeds when player is at a safe distance >= 40m"
      ;; Move player pace to safe distance: (-250, 107, -85) => >100m from base
      (fix/write-player-dat! (clojure.java.io/file (:saves-dir *sandbox*) "bfb04b46-41a1-3e85-b0fb-bc18eccdb3c6.dat")
                             {:x -250.0 :y 107.0 :z -85.0 :yaw 180.0 :health 20.0})
      (let [{:keys [status body]} (th/http-post "/api/nuke/launch?player=pace")]
        (is (= 200 status))
        (is (true? (:ok body)))
        ;; Wait briefly for background nuke thread (MC_FAST_TIMERS=1)
        (Thread/sleep 150)
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          ;; Immunity granted
          (is (some #(re-find #"effect give @a resistance 15 5 true" %) cmds))
          ;; Crater excavation
          (is (some #(re-find #"fill ~-16 ~-2 ~-16 ~16 ~12 ~16 air replace" %) cmds))
          ;; Cluster detonation
          (is (some #(re-find #"ExplosionRadius:18b" %) cmds)))))))

(deftest test-underground-monster-purge
  (with-sandbox-server
    (testing "Underground monster purge targets caves while protecting pets, golems, and players"
      (let [{:keys [status body]} (th/http-post "/api/monsters/purge-underground?player=pace")]
        (is (= 200 status))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"kill @e\[.*type=!minecraft:player.*type=!minecraft:iron_golem.*type=!minecraft:wolf" %) cmds))
          ;; All 5 layers present
          (is (some #(re-find #"positioned -146 80 -85" %) cmds))
          (is (some #(re-find #"positioned -146 -50 -85" %) cmds)))))))

(deftest test-thermal-pool-and-enderman-totem-lifecycles
  (with-sandbox-server
    (testing "Thermal pool and Enderman totem build and undo cycles"
      ;; Thermal pool build & undo
      (let [{:keys [status body]} (th/http-post "/api/build/thermal-pool?player=pace")]
        (is (= 200 status))
        (is (true? (:ok body))))
      (let [{:keys [status body]} (th/http-post "/api/build/undo-thermal-pool")]
        (is (= 200 status))
        (is (true? (:ok body))))

      ;; Totem build & undo
      (let [{:keys [status body]} (th/http-post "/api/build/enderman-totem?player=pace")]
        (is (= 200 status))
        (is (true? (:ok body))))
      (let [{:keys [status body]} (th/http-post "/api/build/undo-enderman-totem")]
        (is (= 200 status))
        (is (true? (:ok body)))))))

(deftest test-lava-fill-and-undo-contracts
  (with-sandbox-server
    (testing "Fill with lava executes safely and undo drains lava"
      ;; Fill lava
      (let [{:keys [status body]} (th/http-post "/api/build/lava?player=pace&radius=4&depth=3")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (is (re-find #"lava" (:message body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"effect give pace fire_resistance" %) cmds))
          (is (some #(re-find #"fill .* lava replace air" %) cmds))))

      ;; Undo lava
      (let [{:keys [status body]} (th/http-post "/api/build/undo-lava")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"fill .* air replace lava" %) cmds)))))))

(deftest test-stone-castle-and-undo-contracts
  (with-sandbox-server
    (testing "Medieval stone castle builds authentic 15x15 fortress and undo restores terrain"
      ;; Build stone castle
      (let [{:keys [status body]} (th/http-post "/api/build/castle?player=pace")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (is (re-find #"Castelo Medieval de Pedra" (:message body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          ;; Curtains & towers
          (is (some #(re-find #"fill .* stone_bricks replace" %) cmds))
          (is (some #(re-find #"fill .* stone_brick_wall replace" %) cmds))
          (is (some #(re-find #"fill .* iron_bars replace" %) cmds))
          (is (some #(re-find #"setblock .* chiseled_stone_bricks replace" %) cmds))
          (is (some #(re-find #"tp pace" %) cmds))))

      ;; Undo stone castle
      (let [{:keys [status body]} (th/http-post "/api/build/undo-castle")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"fill .* grass_block replace" %) cmds)))))))

(deftest test-titanic-castle-and-undo-contracts
  (with-sandbox-server
    (testing "Titanic Void Citadel raises a double-walled 41x41 fortress with moat, towers and spire"
      ;; Build titanic castle
      (let [{:keys [status body]} (th/http-post "/api/build/titanic-castle?player=pace")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (is (re-find #"Castelo Titânico do Vazio" (:message body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          ;; Double curtain: stone_bricks (outer) + deepslate_bricks (inner)
          (is (some #(re-find #"fill .* stone_bricks replace" %) cmds))
          (is (some #(re-find #"fill .* deepslate_bricks replace" %) cmds))
          ;; Moat water + drawbridge planks
          (is (some #(re-find #"fill .* water replace" %) cmds))
          (is (some #(re-find #"fill .* oak_planks replace" %) cmds))
          ;; Creatures of detail: beacons, stained glass, garden flora
          (is (some #(re-find #"beacon replace" %) cmds))
          (is (some #(re-find #"stained_glass replace" %) cmds))
          (is (some #(re-find #"lily_pad replace" %) cmds))
          (is (some #(re-find #"gold_block replace" %) cmds))
          ;; Player safely placed on the keep crown
          (is (some #(re-find #"tp pace -146 .* -85" %) cmds))))

      ;; Undo titanic castle
      (let [{:keys [status body]} (th/http-post "/api/build/undo-titanic-castle")]
        (is (= 200 status))
        (is (schemas/valid? schemas/ActionResponse body))
        (is (true? (:ok body)))
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          (is (some #(re-find #"fill .* air replace" %) cmds))
          (is (some #(re-find #"fill .* grass_block replace" %) cmds)))))))
