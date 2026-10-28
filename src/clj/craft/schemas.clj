(ns craft.schemas
  "Malli schemas formalizing data contracts for all Minecraft Dashboard & REPL APIs."
  (:require [malli.core :as m]))

;; ---------------------------------------------------------------------------
;; Telemetry Schemas (/api/telemetry)
;; ---------------------------------------------------------------------------

(def PlayerTelemetry
  [:map
   [:name :string]
   [:x :double]
   [:y :double]
   [:z :double]
   [:yaw :double]
   [:pitch :double]
   [:health :double]])

(def Waypoint
  [:map
   [:name :string]
   [:x :int]
   [:y :int]
   [:z :int]
   [:icon {:optional true} :string]])

(def TelemetryResponse
  [:map
   [:server_online :boolean]
   [:players [:vector PlayerTelemetry]]
   [:waypoints [:vector Waypoint]]])

;; ---------------------------------------------------------------------------
;; Inventory Schemas (/api/inventory)
;; ---------------------------------------------------------------------------

(def InventoryItem
  [:map
   [:id :string]
   [:name :string]
   [:count :int]
   [:slot :int]
   [:icon :string]
   [:damage :int]])

(def ArmorMap
  [:map
   [:helmet [:maybe InventoryItem]]
   [:chestplate [:maybe InventoryItem]]
   [:leggings [:maybe InventoryItem]]
   [:boots [:maybe InventoryItem]]])

(def InventoryResponse
  [:map
   [:player :string]
   [:health :double]
   [:food :int]
   [:xp :int]
   [:hotbar [:vector [:maybe InventoryItem]]]
   [:main [:vector [:maybe InventoryItem]]]
   [:armor ArmorMap]
   [:offhand [:maybe InventoryItem]]
   [:total_items :int]])

;; ---------------------------------------------------------------------------
;; Actions & Gear / Chests Schemas
;; ---------------------------------------------------------------------------

(def ActionResponse
  [:map
   [:ok :boolean]
   [:message {:optional true} :string]
   [:output {:optional true} :string]
   [:error {:optional true} :string]])

(def RepairResponse
  [:map
   [:ok :boolean]
   [:message :string]
   [:count :int]])

(def DepositBatch
  [:map
   [:item :string]
   [:count :int]
   [:chest :string]])

(def DepositResponse
  [:map
   [:ok :boolean]
   [:message :string]
   [:deposited [:vector DepositBatch]]])

(def BlueprintBounds
  [:map
   [:min_x :int]
   [:max_x :int]
   [:min_y :int]
   [:max_y :int]
   [:min_z :int]
   [:max_z :int]
   [:size_x :int]
   [:size_y :int]
   [:size_z :int]])

(def BlueprintMetaResponse
  [:map
   [:ok :boolean]
   [:message :string]
   [:meta [:map
           [:name :string]
           [:timestamp :string]
           [:bounds BlueprintBounds]
           [:stats [:map
                    [:total_blocks :int]
                    [:non_air_blocks :int]
                    [:containers_count :int]]]]]])

(defn valid?
  "Checks if value conforms to schema."
  [schema val]
  (m/validate schema val))
