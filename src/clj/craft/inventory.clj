(ns craft.inventory
  "Player inventory parsing, item formatting, and 100% gear repair planning."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [craft.config :as cfg]
            [craft.bridge :as bridge]
            [craft.nbt :as nbt])
  (:import [java.io File]))

(defn get-slot-target [slot]
  (let [s (int (or slot -999))]
    (cond
      (and (>= s 0) (<= s 8)) (str "hotbar." s)
      (and (>= s 9) (<= s 35)) (str "inventory." (- s 9))
      (= s 100) "armor.feet"
      (= s 101) "armor.legs"
      (= s 102) "armor.chest"
      (= s 103) "armor.head"
      (= s -106) "weapon.offhand"
      :else nil)))

(defn item-icon [raw-id]
  (let [id (str/lower-case (or raw-id ""))]
    (cond
      (str/includes? id "sword") "⚔️"
      (str/includes? id "pickaxe") "⛏️"
      (str/includes? id "axe") "🪓"
      (str/includes? id "shovel") "🥄"
      (str/includes? id "hoe") "🧑‍🌾"
      (str/includes? id "helmet") "🪖"
      (str/includes? id "chestplate") "🦺"
      (str/includes? id "leggings") "👖"
      (str/includes? id "boots") "🥾"
      (str/includes? id "shield") "🛡️"
      (or (str/includes? id "bow") (str/includes? id "crossbow")) "🏹"
      (str/includes? id "arrow") "🎯"
      (or (str/includes? id "poppy") (str/includes? id "flower")) "🌺"
      (str/includes? id "lead") "🪢"
      (or (str/includes? id "wheat") (str/includes? id "seed")) "🌾"
      (or (str/includes? id "beef") (str/includes? id "porkchop") (str/includes? id "mutton") (str/includes? id "chicken")) "🥩"
      (str/includes? id "bread") "🍞"
      (str/includes? id "apple") "🍎"
      (str/includes? id "torch") "🕯️"
      (str/includes? id "bucket") "🪣"
      (str/includes? id "potion") "🧪"
      (str/includes? id "diamond") "💎"
      (str/includes? id "emerald") "❇️"
      (or (str/includes? id "gold") (str/includes? id "ingot") (str/includes? id "iron")) "🪙"
      (str/includes? id "bed") "🛏️"
      (or (str/includes? id "stone") (str/includes? id "cobblestone")) "🧱"
      (or (str/includes? id "wood") (str/includes? id "planks") (str/includes? id "log")) "🪵"
      (str/includes? id "sand") "⏳"
      (str/includes? id "scute") "🛡️"
      (str/includes? id "rod") "🎣"
      (str/includes? id "coal") "⬛"
      (str/includes? id "stick") "🥢"
      (str/includes? id "saddle") "🏇"
      (or (str/includes? id "fence") (str/includes? id "wall")) "🪜"
      (str/includes? id "carrot") "🥕"
      (str/includes? id "wool") "🧶"
      :else "📦")))

(defn format-item-name [raw-id]
  (-> (or raw-id "")
      (str/replace #"^minecraft:" "")
      (str/replace #"_" " ")
      (str/split #"\s+")
      (->> (map str/capitalize)
           (str/join " "))))

(defn format-item [item-map]
  (when item-map
    (let [raw-id (str (get item-map "id" ""))
          item-name (format-item-name raw-id)
          count-val (int (get item-map "count" 1))
          slot (int (get item-map "Slot" 0))
          comp (get item-map "components" {})
          damage (int (get comp "minecraft:damage" 0))]
      {:id raw-id
       :name item-name
       :count count-val
       :slot slot
       :icon (item-icon raw-id)
       :damage damage})))

(defn get-player-dat-file [player-name]
  (let [p-key (str/lower-case (or player-name "pace"))
        uuid (get cfg/player-uuids p-key)]
    (when uuid
      (let [f (io/file (cfg/saves-dir) (str uuid ".dat"))]
        (when (.exists f) f)))))

(defn read-player-nbt [player-name]
  (when-let [dat-file (get-player-dat-file player-name)]
    (try
      (nbt/read-gzip-nbt dat-file)
      (catch Exception _ nil))))

(defn get-player-inventory [player-name]
  (if-let [nbt-data (read-player-nbt player-name)]
    (let [raw-inv (get nbt-data "Inventory" [])
          main-inv (vec (repeat 36 nil))
          armor-inv {:head nil :chest nil :legs nil :feet nil}
          offhand (atom nil)
          
          ;; Populate
          [main-res armor-res]
          (reduce
           (fn [[m a] raw-item]
             (let [f (format-item raw-item)
                   s (:slot f)]
               (cond
                 (and (>= s 0) (<= s 35))
                 [(assoc m s f) a]

                 (= s 103) [m (assoc a :head f)]
                 (= s 102) [m (assoc a :chest f)]
                 (= s 101) [m (assoc a :legs f)]
                 (= s 100) [m (assoc a :feet f)]
                 (= s -106) (do (reset! offhand f) [m a])
                 :else [m a])))
           [main-inv armor-inv]
           raw-inv)]
      {:player player-name
       :main main-res
       :armor armor-res
       :offhand @offhand
       :health (double (get nbt-data "Health" 20.0))
       :food (int (get nbt-data "foodLevel" 20))
       :xp (int (get nbt-data "XpLevel" 0))})
    {:error "Jogador não encontrado no save"}))

(defn plan-gear-repair
  "Pure function: computes the list of /item replace commands to repair damaged gear."
  [player-name inventory-list]
  (let [damaged (filter (fn [item]
                          (let [comp (get item "components" {})
                                dmg (int (get comp "minecraft:damage" 0))
                                slot (get item "Slot")]
                            (and (pos? dmg) (get-slot-target slot))))
                        inventory-list)]
    (mapv
     (fn [item]
       (let [slot (get item "Slot")
             slot-target (get-slot-target slot)
             item-id (get item "id")
             count-val (get item "count" 1)
             comp (get item "components" {})
             ench-levels (get-in comp ["minecraft:enchantments" "levels"])
             cname (get comp "minecraft:custom_name")
             comp-parts (cond-> []
                          (seq ench-levels)
                          (conj (str "enchantments={levels:{"
                                     (str/join "," (map (fn [[k v]]
                                                          (str (str/replace (name k) #"^minecraft:" "") ":" v))
                                                        ench-levels))
                                     "}}"))
                          cname
                          (conj (str "custom_name='" cname "'")))
             comp-str (if (seq comp-parts)
                        (str "[" (str/join "," comp-parts) "]")
                        "")
             cmd (str "item replace entity " player-name " " slot-target " with " item-id comp-str " " count-val)]
         {:cmd cmd
          :name (format-item-name item-id)}))
     damaged)))

(defn repair-player-gear! [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt-data (read-player-nbt player-name)]
    (let [inv (get nbt-data "Inventory" [])
          repair-plan (plan-gear-repair player-name inv)]
      (if (empty? repair-plan)
        [true "Todos os itens e equipamentos já estão com 100% de durabilidade!" 0]
        (do
          (doseq [{:keys [cmd]} repair-plan]
            (bridge/run-command! cmd))
          (bridge/run-command! "save-all")
          (let [names (map :name repair-plan)
                cnt (count repair-plan)
                msg (str "Reparado(s) " cnt " item(ns) para 100%: " (str/join ", " names))]
            [true msg cnt]))))
    [false "Jogador não encontrado no save" 0]))
