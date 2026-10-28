(ns craft.inventory-chests-contract-test
  "Contract and invariant verification for Inventory, Gear Repair and Smart Chest Deposit."
  (:require [clojure.test :refer [deftest is testing]]
            [craft.fixtures :as fix]
            [craft.schemas :as schemas]
            [craft.test-helper :as th :refer [with-sandbox-server *sandbox*]]))

(deftest test-inventory-contract-and-parsing
  (with-sandbox-server
    (testing "GET /api/inventory returns valid schema and correct slot mappings"
      (let [{:keys [status body]} (th/http-get "/api/inventory?player=pace")]
        (is (= 200 status))
        (is (schemas/valid? schemas/InventoryResponse body))
        (is (= "pace" (:player body)))
        (is (= 20.0 (:health body)))
        ;; Hotbar slot 0 has diamond sword
        (let [sword (get-in body [:hotbar 0])]
          (is (= "minecraft:diamond_sword" (:id sword)))
          (is (= 0 (:slot sword)))
          (is (= 15 (:damage sword))))
        ;; Elytra is in chestplate slot
        (let [elytra (get-in body [:armor :chestplate])]
          (is (= "minecraft:elytra" (:id elytra)))
          (is (= 25 (:damage elytra))))))))

(deftest test-gear-repair-invariants
  (with-sandbox-server
    (testing "POST /api/gear/repair restores damaged items while keeping enchantments and custom name"
      (let [{:keys [status body]} (th/http-post "/api/gear/repair?player=pace")]
        (is (= 200 status))
        (is (schemas/valid? schemas/RepairResponse body))
        (is (true? (:ok body)))
        ;; Sword (dmg 15) and Elytra (dmg 25) were damaged in fixture
        (is (= 3 (:count body))) ;; sword, elytra, and diamond chestplate (dmg 50)
        
        ;; Check recorded dry-run commands
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))
              repair-cmds (filter #(re-find #"item replace entity pace" %) cmds)]
          (is (seq repair-cmds))
          ;; Custom name preserved
          (is (some #(re-find #"custom_name='Lamina Ancestral'" %) repair-cmds))
          ;; Enchantments preserved without 'minecraft:' prefix
          (is (some #(re-find #"protection:4" %) repair-cmds)))))))

(deftest test-smart-chest-deposit-invariants
  (with-sandbox-server
    (testing "POST /api/chests/deposit respects all safety rules and forceload boundaries"
      (let [{:keys [status body]} (th/http-post "/api/chests/deposit?player=pace")]
        (is (= 200 status))
        (is (schemas/valid? schemas/DepositResponse body))
        (is (true? (:ok body)))
        
        (let [cmds (mapv :cmd (fix/read-recorded-commands! *sandbox*))]
          ;; Invariant 1: Forceload must wrap operation
          (is (some #(re-find #"forceload add -165 -95 -145 -75" %) cmds))
          (is (some #(re-find #"forceload remove -165 -95 -145 -75" %) cmds))

          ;; Invariant 2: Hotbar (slot 0 sword, slot 1 pickaxe) must NEVER be cleared
          (is (not-any? #(re-find #"clear pace minecraft:diamond_sword" %) cmds))
          (is (not-any? #(re-find #"clear pace minecraft:iron_pickaxe" %) cmds))

          ;; Invariant 3: Armor (elytra) must NEVER be cleared
          (is (not-any? #(re-find #"clear pace minecraft:elytra" %) cmds))

          ;; Invariant 4: Enchanted chestplate in main inventory (slot 11) must NEVER be cleared
          (is (not-any? #(re-find #"clear pace minecraft:diamond_chestplate" %) cmds))

          ;; Invariant 5: Raw materials in main inventory (iron_ingot 32 in slot 9, stone 64 in slot 10) are deposited
          (is (some #(re-find #"clear pace minecraft:iron_ingot" %) cmds))
          (is (some #(re-find #"clear pace minecraft:stone" %) cmds)))))))
