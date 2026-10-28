(ns craft.chests
  "Smart chest deposit engine for sorting base loot with strictly protected invariants."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [craft.bridge :as bridge]
            [craft.config :as cfg]
            [craft.inventory :as inv]))

(def base-chests
  [{:name "Minérios & Metais"
    :coords [-160 107 -89]
    :keys ["coal" "iron" "gold" "copper" "redstone" "diamond" "emerald" "lapis" "raw_iron" "raw_copper" "raw_gold"]}
   {:name "Minérios (Reserva)"
    :coords [-160 107 -88]
    :keys ["coal" "iron" "gold" "copper" "redstone" "diamond" "emerald" "lapis" "quartz" "amethyst"]}
   {:name "Pedras & Rochas"
    :coords [-156 107 -81]
    :keys ["stone" "cobblestone" "granite" "diorite" "andesite" "gravel" "flint" "dirt" "sand" "sandstone" "calcite" "tuff" "dripstone"]}
   {:name "Deepslate & Terras"
    :coords [-155 107 -81]
    :keys ["deepslate" "cobbled_deepslate" "tuff" "mud" "clay" "rooted_dirt"]}
   {:name "Pedras Especiais"
    :coords [-154 107 -81]
    :keys ["obsidian" "crying_obsidian" "amethyst" "basalt" "blackstone"]}
   {:name "Madeiras & Flora"
    :coords [-153 107 -90]
    :keys ["log" "planks" "wood" "sapling" "leaves" "stick" "door" "fence" "gate" "trapdoor" "ladder" "slab" "stairs" "bamboo"]}
   {:name "Comida & Agricultura"
    :coords [-153 107 -89]
    :keys ["beef" "porkchop" "mutton" "chicken" "rabbit" "cod" "salmon" "wheat" "seed" "carrot" "potato" "beetroot" "apple" "bread" "melon" "cactus" "sugar" "egg" "mushroom" "berry" "honey"]}
   {:name "Drops de Monstros"
    :coords [-145 107 -87]
    :keys ["rotten_flesh" "bone" "gunpowder" "string" "spider_eye" "arrow" "feather" "leather" "slime" "scute" "snowball" "ender_pearl" "blaze_rod" "ghast_tear"]}])

(def protected-items
  #{"sword" "pickaxe" "axe" "shovel" "hoe" "helmet" "chestplate" "leggings" "boots"
    "elytra" "shield" "bow" "crossbow" "fishing_rod" "flint_and_steel" "firework_rocket"
    "water_bucket" "lava_bucket" "torch" "compass" "clock" "totem_of_undying" "spyglass"})

(defn find-target-chest [raw-id]
  (let [clean (-> (or raw-id "")
                  (str/replace #"^minecraft:" "")
                  str/lower-case)]
    (first
     (filter
      (fn [chest]
        (some #(str/includes? clean %) (:keys chest)))
      base-chests))))

(defn parse-chest-slots [x y z]
  (bridge/run-command! (str "data get block " x " " y " " z " Items"))
  (let [log-f (cfg/log-file)
        line (when (.exists log-f)
               (with-open [rdr (io/reader log-f)]
                 (let [lines (->> (line-seq rdr)
                                  (take-last 35)
                                  reverse)
                       needle (str x ", " y ", " z " has the following block data:")]
                   (some #(when (str/includes? % needle)
                            (second (str/split % #"block data:")))
                         lines))))
        slots (atom {})]
    (when line
      (doseq [m (re-seq #"\{([^\{\}]*)\}" line)]
        (let [block-data (second m)
              s-m (re-find #"Slot:\s*(\d+)b" block-data)
              id-m (re-find #"id:\s*\"([^\"]+)\"" block-data)
              c-m (re-find #"count:\s*(\d+)" block-data)]
          (when (and s-m id-m)
            (let [s (Integer/parseInt (second s-m))
                  id (second id-m)
                  cnt (if c-m (Integer/parseInt (second c-m)) 1)]
              (swap! slots assoc s {:id id :count cnt}))))))
    @slots))

(defn plan-chest-deposit
  "Pure function: plan smart deposits into base chests preserving all invariants."
  [player-name inventory-list chest-cache-fn]
  (let [valid-items (filter
                     (fn [item]
                       (let [slot (int (get item "Slot" 0))
                             raw-id (get item "id" "")
                             clean-id (-> raw-id (str/replace #"^minecraft:" "") str/lower-case)
                             comp (get item "components" {})]
                         (and (>= slot 9)
                              (<= slot 35)
                              (not (some #(str/includes? clean-id %) protected-items))
                              (not (get comp "minecraft:enchantments"))
                              (find-target-chest raw-id))))
                     inventory-list)
        chest-caches (atom {})
        commands (atom [])
        deposited (atom [])]
    (doseq [item valid-items]
      (let [item-id (get item "id")
            clean-id (-> item-id (str/replace #"^minecraft:" "") str/lower-case)
            chest (find-target-chest item-id)
            [cx cy cz] (:coords chest)]
        (when-not (contains? @chest-caches [cx cy cz])
          (swap! chest-caches assoc [cx cy cz] (chest-cache-fn cx cy cz)))
        
        (let [rem (atom (int (get item "count" 1)))
              c-slots (get @chest-caches [cx cy cz])]
          
          ;; 1. Stack into existing slots with same item and count < 64
          (doseq [[s s-data] (sort-by key c-slots)]
            (when (and (pos? @rem)
                       (= (:id s-data) item-id)
                       (< (int (:count s-data)) 64))
              (let [avail (- 64 (int (:count s-data)))
                    add (min avail @rem)
                    new-c (+ (int (:count s-data)) add)]
                (swap! chest-caches assoc-in [[cx cy cz] s :count] new-c)
                (swap! commands conj (str "item replace block " cx " " cy " " cz " container." s " with " item-id " " new-c))
                (swap! commands conj (str "clear " player-name " " item-id " " add))
                (swap! deposited conj {:item clean-id :count add :chest (:name chest)})
                (swap! rem - add))))

          ;; 2. Place into lowest empty slots 0..26
          (when (pos? @rem)
            (doseq [s (range 27)]
              (when (and (pos? @rem)
                         (not (contains? (get @chest-caches [cx cy cz]) s)))
                (let [add (min 64 @rem)]
                  (swap! chest-caches assoc-in [[cx cy cz] s] {:id item-id :count add})
                  (swap! commands conj (str "item replace block " cx " " cy " " cz " container." s " with " item-id " " add))
                  (swap! commands conj (str "clear " player-name " " item-id " " add))
                  (swap! deposited conj {:item clean-id :count add :chest (:name chest)})
                  (swap! rem - add))))))))
    {:commands @commands
     :deposited @deposited}))

(defn deposit-player-to-base-chests! [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt-data (inv/read-player-nbt player-name)]
    (do
      (bridge/run-command! "forceload add -165 -95 -145 -75")
      (try
        (let [inv (get nbt-data "Inventory" [])
              {:keys [commands deposited]} (plan-chest-deposit player-name inv parse-chest-slots)]
          (if (empty? deposited)
            [true "Nenhum item avulso ou acumulável para depositar (equipamentos e hotbar protegidos)!" []]
            (do
              (doseq [cmd commands]
                (bridge/run-command! cmd))
              (bridge/run-command! "save-all")
              (let [total (reduce + (map :count deposited))
                    batches (count deposited)
                    summary (str "📦 Guardado(s) " total " item(ns) em " batches " lote(s) nos baús da base!")]
                [true summary deposited]))))
        (finally
          (bridge/run-command! "forceload remove -165 -95 -145 -75"))))
    [false "Jogador não encontrado no save" []]))
