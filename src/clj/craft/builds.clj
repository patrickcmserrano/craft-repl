(ns craft.builds
  "Geometric architectural builders, safety controls, undo history, and nuke detonation."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [craft.bridge :as bridge]
            [craft.config :as cfg]
            [craft.inventory :as inv]))

;; --- UNDO STATE PERSISTENCE ---

(defn- undo-file ^java.io.File []
  (io/file (cfg/config-dir) "undo_state.edn"))

(defn get-undo-state []
  (let [f (undo-file)]
    (if (.exists f)
      (try (edn/read-string (slurp f)) (catch Exception _ {}))
      {})))

(defn save-undo-state! [k v]
  (let [cur (get-undo-state)
        nxt (if v (assoc cur k v) (dissoc cur k))]
    (spit (undo-file) (pr-str nxt))))

;; --- PISCINA TERMAL (ONSEN) ---

(defn build-thermal-pool! [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt (inv/read-player-nbt player-name)]
    (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
          cx (int (Math/round (double (first pos))))
          cy (int (Math/round (double (second pos))))
          cz (int (Math/round (double (nth pos 2))))
          cmds
          [(str "fill " (- cx 3) " " (- cy 3) " " (- cz 3) " " (+ cx 3) " " (+ cy 2) " " (+ cz 3) " air")
           (str "fill " (- cx 3) " " (- cy 3) " " (- cz 3) " " (+ cx 3) " " (- cy 2) " " (+ cz 3) " smooth_stone")
           (str "setblock " cx " " (- cy 3) " " cz " repeating_command_block{auto:1b,Command:\"execute positioned ~ ~3 ~ as @a[distance=..3.8] run effect give @s minecraft:regeneration 3 1 true\"} replace")
           (str "setblock " (+ cx 1) " " (- cy 3) " " cz " repeating_command_block{auto:1b,Command:\"execute positioned ~-1 ~3 ~ as @a[distance=..3.8] run effect clear @s minecraft:poison\"} replace")
           (str "setblock " (- cx 1) " " (- cy 3) " " cz " repeating_command_block{auto:1b,Command:\"execute positioned ~1 ~3 ~ as @a[distance=..3.8] run effect clear @s minecraft:wither\"} replace")
           (str "setblock " cx " " (- cy 3) " " (+ cz 1) " repeating_command_block{auto:1b,Command:\"execute positioned ~ ~3 ~-1 as @a[distance=..3.8] run effect clear @s minecraft:slowness\"} replace")
           (str "setblock " cx " " (- cy 3) " " (- cz 1) " repeating_command_block{auto:1b,Command:\"execute positioned ~ ~3 ~1 as @a[distance=..3.8] run effect give @s minecraft:saturation 2 0 true\"} replace")
           (str "setblock " (+ cx 1) " " (- cy 2) " " (+ cz 1) " campfire[lit=true] replace")
           (str "setblock " (- cx 1) " " (- cy 2) " " (- cz 1) " campfire[lit=true] replace")
           (str "setblock " (+ cx 1) " " (- cy 2) " " (- cz 1) " campfire[lit=true] replace")
           (str "setblock " (- cx 1) " " (- cy 2) " " (+ cz 1) " campfire[lit=true] replace")
           (str "fill " (- cx 2) " " (- cy 1) " " (- cz 2) " " (+ cx 2) " " (- cy 1) " " (+ cz 2) " smooth_quartz replace")
           (str "setblock " cx " " (- cy 1) " " cz " soul_sand replace")
           (str "setblock " (+ cx 1) " " (- cy 1) " " cz " prismarine_bricks replace")
           (str "setblock " (- cx 1) " " (- cy 1) " " cz " prismarine_bricks replace")
           (str "setblock " cx " " (- cy 1) " " (+ cz 1) " prismarine_bricks replace")
           (str "setblock " cx " " (- cy 1) " " (- cz 1) " prismarine_bricks replace")
           (str "fill " (- cx 3) " " cy " " (- cz 2) " " (- cx 3) " " cy " " (+ cz 2) " smooth_quartz_stairs[facing=east] replace")
           (str "fill " (+ cx 3) " " cy " " (- cz 2) " " (+ cx 3) " " cy " " (+ cz 2) " smooth_quartz_stairs[facing=west] replace")
           (str "fill " (- cx 2) " " cy " " (- cz 3) " " (+ cx 2) " " cy " " (- cz 3) " smooth_quartz_stairs[facing=south] replace")
           (str "fill " (- cx 2) " " cy " " (+ cz 3) " " (+ cx 2) " " cy " " (+ cz 3) " smooth_quartz_stairs[facing=north] replace")
           (str "setblock " (- cx 3) " " cy " " (- cz 3) " chiseled_quartz_block replace")
           (str "setblock " (- cx 3) " " (+ cy 1) " " (- cz 3) " soul_lantern replace")
           (str "setblock " (+ cx 3) " " cy " " (- cz 3) " chiseled_quartz_block replace")
           (str "setblock " (+ cx 3) " " (+ cy 1) " " (- cz 3) " soul_lantern replace")
           (str "setblock " (- cx 3) " " cy " " (+ cz 3) " chiseled_quartz_block replace")
           (str "setblock " (- cx 3) " " (+ cy 1) " " (+ cz 3) " soul_lantern replace")
           (str "setblock " (+ cx 3) " " cy " " (+ cz 3) " chiseled_quartz_block replace")
           (str "setblock " (+ cx 3) " " (+ cy 1) " " (+ cz 3) " soul_lantern replace")
           (str "fill " (- cx 2) " " cy " " (- cz 2) " " (+ cx 2) " " cy " " (+ cz 2) " water replace")
           (str "setblock " cx " " cy " " (+ cz 3) " smooth_quartz_slab replace")
           (str "setblock " cx " " cy " " (+ cz 4) " polished_blackstone_pressure_plate replace")
           (str "playsound minecraft:block.bell.use master @a " cx " " cy " " cz " 1 1.2")]]
      (save-undo-state! :thermal-pool {:coords [cx cy cz] :player player-name})
      (doseq [c cmds] (bridge/run-command! c))
      [true (str "♨️ Piscina Termal Curativa erguida com sucesso em [" cx ", " cy ", " cz "]!")])
    [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")]))

(defn undo-thermal-pool! []
  (if-let [state (:thermal-pool (get-undo-state))]
    (let [[cx cy cz] (:coords state)
          cmds [(str "fill " (- cx 3) " " (- cy 3) " " (- cz 3) " " (+ cx 3) " " (+ cy 2) " " (+ cz 3) " air replace")
                (str "fill " (- cx 3) " " (- cy 3) " " (- cz 3) " " (+ cx 3) " " (- cy 1) " " (+ cz 3) " dirt replace")
                (str "fill " (- cx 3) " " cy " " (- cz 3) " " (+ cx 3) " " cy " " (+ cz 3) " grass_block replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :thermal-pool nil)
      [true (str "↩️ Piscina termal em [" cx ", " cy ", " cz "] desfeita e terreno restaurado!")])
    [false "Nenhuma piscina termal recente encontrada para desfazer."]))

;; --- OBELISCO ANTI-ENDERMAN ---

(defn build-enderman-totem! [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt (inv/read-player-nbt player-name)]
    (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
          cx (int (Math/round (double (first pos))))
          cy (int (Math/round (double (second pos))))
          cz (int (Math/round (double (nth pos 2))))
          cmds
          [(str "fill " (- cx 1) " " (- cy 2) " " (- cz 1) " " (+ cx 1) " " (+ cy 6) " " (+ cz 1) " air")
           (str "setblock " cx " " (- cy 2) " " cz " repeating_command_block{auto:1b,Command:\"execute as @e[type=enderman,distance=..45] at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1.5 0.8\"} replace")
           (str "setblock " (+ cx 1) " " (- cy 2) " " cz " repeating_command_block{auto:1b,Command:\"execute as @e[type=enderman,distance=..45] at @s run particle portal ~ ~1 ~ 1 1 1 0.5 50\"} replace")
           (str "setblock " (- cx 1) " " (- cy 2) " " cz " repeating_command_block{auto:1b,Command:\"execute as @e[type=enderman,distance=..45] at @s run tp @s ~120 ~ ~120\"} replace")
           (str "setblock " cx " " (- cy 2) " " (+ cz 1) " repeating_command_block{auto:1b,Command:\"particle reverse_portal ~ ~4 ~-1 0.5 1.2 0.5 0.02 6\"} replace")
           (str "fill " (- cx 1) " " (- cy 1) " " (- cz 1) " " (+ cx 1) " " (- cy 1) " " (+ cz 1) " polished_deepslate replace")
           (str "setblock " cx " " cy " " cz " crying_obsidian replace")
           (str "setblock " (+ cx 1) " " cy " " cz " polished_deepslate_stairs[facing=west] replace")
           (str "setblock " (- cx 1) " " cy " " cz " polished_deepslate_stairs[facing=east] replace")
           (str "setblock " cx " " cy " " (+ cz 1) " polished_deepslate_stairs[facing=north] replace")
           (str "setblock " cx " " cy " " (- cz 1) " polished_deepslate_stairs[facing=south] replace")
           (str "setblock " (+ cx 1) " " cy " " (+ cz 1) " chiseled_deepslate replace")
           (str "setblock " (- cx 1) " " cy " " (+ cz 1) " chiseled_deepslate replace")
           (str "setblock " (+ cx 1) " " cy " " (- cz 1) " chiseled_deepslate replace")
           (str "setblock " (- cx 1) " " cy " " (- cz 1) " chiseled_deepslate replace")
           (str "setblock " cx " " (+ cy 1) " " cz " crying_obsidian replace")
           (str "setblock " cx " " (+ cy 2) " " cz " sea_lantern replace")
           (str "setblock " cx " " (+ cy 3) " " cz " amethyst_block replace")
           (str "setblock " cx " " (+ cy 4) " " cz " end_rod[facing=up] replace")
           (str "setblock " cx " " (+ cy 5) " " cz " amethyst_cluster[facing=up] replace")
           (str "setblock " (+ cx 1) " " (+ cy 1) " " (+ cz 1) " soul_lantern replace")
           (str "setblock " (- cx 1) " " (+ cy 1) " " (+ cz 1) " soul_lantern replace")
           (str "setblock " (+ cx 1) " " (+ cy 1) " " (- cz 1) " soul_lantern replace")
           (str "setblock " (- cx 1) " " (+ cy 1) " " (- cz 1) " soul_lantern replace")
           (str "setblock " (+ cx 1) " " (+ cy 2) " " cz " amethyst_cluster[facing=east] replace")
           (str "setblock " (- cx 1) " " (+ cy 2) " " cz " amethyst_cluster[facing=west] replace")
           (str "setblock " cx " " (+ cy 2) " " (+ cz 1) " amethyst_cluster[facing=south] replace")
           (str "setblock " cx " " (+ cy 2) " " (- cz 1) " amethyst_cluster[facing=north] replace")
           (str "playsound minecraft:block.amethyst_block.resonate master @a " cx " " cy " " cz " 2 0.8")
           (str "playsound minecraft:block.conduit.activate master @a " cx " " cy " " cz " 2 1.2")
           "title @a times 5 35 10"
           "title @a title {\"text\":\"🔮 OBELISCO ANTI-ENDERMAN 🔮\",\"color\":\"light_purple\",\"bold\":true}"
           "title @a subtitle {\"text\":\"Campo Repulsor Místico Ativado (Raio 45m)!\",\"color\":\"aqua\"}"]]
      (save-undo-state! :enderman-totem {:coords [cx cy cz] :player player-name})
      (doseq [c cmds] (bridge/run-command! c))
      [true (str "🔮 Obelisco Anti-Enderman erguido com sucesso em [" cx ", " cy ", " cz "]!")])
    [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")]))

(defn undo-enderman-totem! []
  (if-let [state (:enderman-totem (get-undo-state))]
    (let [[cx cy cz] (:coords state)
          cmds [(str "fill " (- cx 1) " " (- cy 2) " " (- cz 1) " " (+ cx 1) " " (+ cy 6) " " (+ cz 1) " air replace")
                (str "fill " (- cx 1) " " (- cy 2) " " (- cz 1) " " (+ cx 1) " " (- cy 1) " " (+ cz 1) " dirt replace")
                (str "fill " (- cx 1) " " cy " " (- cz 1) " " (+ cx 1) " " cy " " (+ cz 1) " grass_block replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :enderman-totem nil)
      [true (str "↩️ Obelisco Anti-Enderman em [" cx ", " cy ", " cz "] desfeito e terreno restaurado!")])
    [false "Nenhum obelisco anti-enderman recente encontrado para desfazer."]))

;; --- CASA PARAMÉTRICA ---

(defn normalize-yaw [yaw]
  (let [y (double (or yaw 0.0))]
    (mod (+ (mod y 360.0) 360.0) 360.0)))

(defn yaw->cardinal [yaw]
  (let [ny (normalize-yaw yaw)]
    (cond
      (and (>= ny 45.0) (< ny 135.0)) "west"
      (and (>= ny 135.0) (< ny 225.0)) "north"
      (and (>= ny 225.0) (< ny 315.0)) "east"
      :else "south")))

(defn build-custom-house!
  ([player-name] (build-custom-house! player-name "cherry" 9 11 4 nil))
  ([player-name wood width length height direction]
   (bridge/run-command! "save-all")
   (if-let [nbt (inv/read-player-nbt player-name)]
     (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
           rot (get nbt "Rotation" [0.0 0.0])
           yaw (first rot)
           norm-yaw (normalize-yaw yaw)
           cx (int (Math/round (double (first pos))))
           cy (int (Math/round (double (second pos))))
           cz (int (Math/round (double (nth pos 2))))
           
           w-type (let [w (str/lower-case (str/trim (or wood "cherry")))]
                    (if (#{"cherry" "spruce" "oak" "birch" "jungle" "acacia" "dark_oak" "mangrove" "bamboo" "crimson" "warped"} w)
                      w
                      "cherry"))
           
           width (max 5 (min 35 (int (or width 9))))
           length (max 5 (min 35 (int (or length 11))))
           height (max 3 (min 12 (int (or height 4))))
           
           dir-map {"norte" "north" "sul" "south" "leste" "east" "oeste" "west"}
           clean-dir (when direction (get dir-map (str/lower-case (str/trim direction)) (str/lower-case (str/trim direction))))
           cardinal (if (#{"north" "south" "east" "west"} clean-dir)
                      clean-dir
                      (yaw->cardinal yaw))
           
           hw (quot width 2)
           hl (quot length 2)
           [min-x max-x min-z max-z]
           (if (#{"south" "north"} cardinal)
             [(- cx hw) (+ cx hw) (- cz hl) (+ cz hl)]
             [(- cx hl) (+ cx hl) (- cz hw) (+ cz hw)])
           
           planks (if (= w-type "bamboo") "bamboo_planks" (str w-type "_planks"))
           log (if (= w-type "bamboo") "stripped_bamboo_block" (str "stripped_" w-type "_log"))
           door (str w-type "_door")
           slab (str w-type "_slab")
           
           base-cmds
           [(str "fill " min-x " " cy " " min-z " " max-x " " (+ cy height 2) " " max-z " air replace")
            (str "fill " min-x " " (- cy 1) " " min-z " " max-x " " (- cy 1) " " max-z " " planks " replace")
            (str "fill " min-x " " cy " " min-z " " max-x " " (+ cy height -1) " " max-z " " planks " outline")
            (str "fill " min-x " " cy " " min-z " " min-x " " (+ cy height -1) " " min-z " " log)
            (str "fill " max-x " " cy " " min-z " " max-x " " (+ cy height -1) " " min-z " " log)
            (str "fill " min-x " " cy " " max-z " " min-x " " (+ cy height -1) " " max-z " " log)
            (str "fill " max-x " " cy " " max-z " " max-x " " (+ cy height -1) " " max-z " " log)
            (str "fill " (- min-x 1) " " (+ cy height) " " (- min-z 1) " " (+ max-x 1) " " (+ cy height) " " (+ max-z 1) " " slab " replace")
            (str "setblock " cx " " (+ cy height -1) " " cz " lantern[hanging=true] replace")]
           
           facing-cmds
           (case cardinal
             "south"
             [(str "setblock " cx " " cy " " max-z " " door "[half=lower,facing=south] replace")
              (str "setblock " cx " " (+ cy 1) " " max-z " " door "[half=upper,facing=south] replace")
              (str "setblock " cx " " (- cy 1) " " (+ max-z 1) " smooth_stone_slab replace")
              (str "setblock " cx " " (+ cy 2) " " (+ max-z 1) " lantern replace")
              (str "fill " (+ min-x 2) " " (+ cy 1) " " min-z " " (- max-x 2) " " (+ cy 2) " " min-z " glass_pane replace " planks)
              (str "fill " min-x " " (+ cy 1) " " (+ min-z 2) " " min-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)
              (str "fill " max-x " " (+ cy 1) " " (+ min-z 2) " " max-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)]
             
             "north"
             [(str "setblock " cx " " cy " " min-z " " door "[half=lower,facing=north] replace")
              (str "setblock " cx " " (+ cy 1) " " min-z " " door "[half=upper,facing=north] replace")
              (str "setblock " cx " " (- cy 1) " " (- min-z 1) " smooth_stone_slab replace")
              (str "setblock " cx " " (+ cy 2) " " (- min-z 1) " lantern replace")
              (str "fill " (+ min-x 2) " " (+ cy 1) " " max-z " " (- max-x 2) " " (+ cy 2) " " max-z " glass_pane replace " planks)
              (str "fill " min-x " " (+ cy 1) " " (+ min-z 2) " " min-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)
              (str "fill " max-x " " (+ cy 1) " " (+ min-z 2) " " max-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)]
             
             "east"
             [(str "setblock " max-x " " cy " " cz " " door "[half=lower,facing=east] replace")
              (str "setblock " max-x " " (+ cy 1) " " cz " " door "[half=upper,facing=east] replace")
              (str "setblock " (+ max-x 1) " " (- cy 1) " " cz " smooth_stone_slab replace")
              (str "setblock " (+ max-x 1) " " (+ cy 2) " " cz " lantern replace")
              (str "fill " min-x " " (+ cy 1) " " (+ min-z 2) " " min-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)
              (str "fill " (+ min-x 2) " " (+ cy 1) " " min-z " " (- max-x 2) " " (+ cy 2) " " min-z " glass_pane replace " planks)
              (str "fill " (+ min-x 2) " " (+ cy 1) " " max-z " " (- max-x 2) " " (+ cy 2) " " max-z " glass_pane replace " planks)]
             
             "west"
             [(str "setblock " min-x " " cy " " cz " " door "[half=lower,facing=west] replace")
              (str "setblock " min-x " " (+ cy 1) " " cz " " door "[half=upper,facing=west] replace")
              (str "setblock " (- min-x 1) " " (- cy 1) " " cz " smooth_stone_slab replace")
              (str "setblock " (- min-x 1) " " (+ cy 2) " " cz " lantern replace")
              (str "fill " max-x " " (+ cy 1) " " (+ min-z 2) " " max-x " " (+ cy 2) " " (- max-z 2) " glass_pane replace " planks)
              (str "fill " (+ min-x 2) " " (+ cy 1) " " min-z " " (- max-x 2) " " (+ cy 2) " " min-z " glass_pane replace " planks)
              (str "fill " (+ min-x 2) " " (+ cy 1) " " max-z " " (- max-x 2) " " (+ cy 2) " " max-z " glass_pane replace " planks)])
           
           all-cmds (concat base-cmds facing-cmds [(str "playsound minecraft:block.wood.place master @a " cx " " cy " " cz " 1 1")])]
       
       (save-undo-state! :custom-house {:min_x (- min-x 1)
                                        :max_x (+ max-x 1)
                                        :min_z (- min-z 1)
                                        :max_z (+ max-z 1)
                                        :cy cy
                                        :h height
                                        :facing cardinal
                                        :player player-name})
       (doseq [c all-cmds] (bridge/run-command! c))
       (let [labels {"south" "Sul (+Z)" "north" "Norte (-Z)" "east" "Leste (+X)" "west" "Oeste (-X)"}
             dir-label (get labels cardinal (str/upper-case cardinal))]
         [true (format "🏠 Casa de %s (%dx%dx%d) erguida voltada para %s (Yaw: %.0f°)! Centro: [%d, %d, %d]."
                       (str/capitalize w-type) width length height dir-label norm-yaw cx cy cz)]))
     [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")])))

(defn undo-custom-house! []
  (if-let [state (:custom-house (get-undo-state))]
    (let [{:keys [min_x max_x min_z max_z cy h facing]} state
          cmds [(str "fill " (- min_x 1) " " (- cy 1) " " (- min_z 1) " " (+ max_x 1) " " (+ cy h 3) " " (+ max_z 1) " air replace")
                (str "fill " (- min_x 1) " " (- cy 1) " " (- min_z 1) " " (+ max_x 1) " " (- cy 1) " " (+ max_z 1) " grass_block replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :custom-house nil)
      [true (str "↩️ Casa orientada para " (str/upper-case (or facing "")) " desfeita e terreno restaurado!")])
    [false "Nenhuma casa recente encontrada no histórico para desfazer."]))

;; --- GRANDE PIRÂMIDE ---

(defn build-pyramid!
  ([cx cy cz] (build-pyramid! cx cy cz 25))
  ([cx cy cz radius]
   (let [rad (int (or radius 25))
         cx (int cx) cy (int cy) cz (int cz)
         cmds (atom [])]
     ;; 1. Base floor
     (swap! cmds conj (str "fill " (- cx rad) " " cy " " (- cz rad) " " (+ cx rad) " " cy " " (+ cz rad) " smooth_sandstone"))
     ;; 2. Stepped hollow layers
     (doseq [i (range 1 (inc rad))]
       (let [r (- rad i)
             y (+ cy i)]
         (if (pos? r)
           (swap! cmds conj (str "fill " (- cx r) " " y " " (- cz r) " " (+ cx r) " " y " " (+ cz r) " sandstone hollow"))
           (do
             (swap! cmds conj (str "setblock " cx " " y " " cz " gold_block"))
             (swap! cmds conj (str "setblock " cx " " (+ y 1) " " cz " sea_lantern"))))))
     ;; 3. Entrance portal
     (swap! cmds conj (str "fill " (- cx 2) " " (+ cy 1) " " (- (+ cz rad) 4) " " (+ cx 2) " " (+ cy 4) " " (+ cz rad) " air"))
     (swap! cmds conj (str "fill " (- cx 3) " " (+ cy 1) " " (+ cz rad) " " (- cx 3) " " (+ cy 5) " " (+ cz rad) " chiseled_sandstone"))
     (swap! cmds conj (str "fill " (+ cx 3) " " (+ cy 1) " " (+ cz rad) " " (+ cx 3) " " (+ cy 5) " " (+ cz rad) " chiseled_sandstone"))
     (swap! cmds conj (str "fill " (- cx 3) " " (+ cy 5) " " (+ cz rad) " " (+ cx 3) " " (+ cy 5) " " (+ cz rad) " cut_sandstone"))
     (swap! cmds conj (str "setblock " (- cx 3) " " (+ cy 6) " " (+ cz rad) " soul_lantern"))
     (swap! cmds conj (str "setblock " (+ cx 3) " " (+ cy 6) " " (+ cz rad) " soul_lantern"))
     ;; 4. Tomb chamber
     (swap! cmds conj (str "fill " (- cx 6) " " (+ cy 1) " " (- cz 6) " " (+ cx 6) " " (+ cy 7) " " (+ cz 6) " air"))
     (swap! cmds conj (str "fill " (- cx 5) " " cy " " (- cz 5) " " (+ cx 5) " " cy " " (+ cz 5) " cut_sandstone"))
     (doseq [px [-4 4] pz [-4 4]]
       (swap! cmds conj (str "fill " (+ cx px) " " (+ cy 1) " " (+ cz pz) " " (+ cx px) " " (+ cy 6) " " (+ cz pz) " chiseled_sandstone"))
       (swap! cmds conj (str "setblock " (+ cx px) " " (+ cy 7) " " (+ cz pz) " sea_lantern")))
     (swap! cmds conj (str "fill " (- cx 1) " " (+ cy 1) " " (- cz 2) " " (+ cx 1) " " (+ cy 1) " " (+ cz 2) " polished_blackstone_bricks"))
     (swap! cmds conj (str "setblock " cx " " (+ cy 2) " " cz " chest[facing=north]{LootTable:\"minecraft:chests/desert_pyramid\"}"))
     (swap! cmds conj (str "setblock " cx " " (+ cy 2) " " (- cz 1) " gold_block"))
     (swap! cmds conj (str "setblock " cx " " (+ cy 2) " " (+ cz 1) " gold_block"))
     (swap! cmds conj "title @a actionbar {\"text\":\"§6🏛️ A Grande Pirâmide Antiga foi erguida!\",\"bold\":true}")
     (swap! cmds conj "playsound minecraft:block.end_portal.spawn ambient @a")
     (doseq [c @cmds] (bridge/run-command! c))
     ;; Register waypoint
     (let [wp-file (io/file (cfg/config-dir) "waypoints.json")
           wps (if (.exists wp-file) (try (json/read-str (slurp wp-file) :key-fn keyword) (catch Exception _ [])) [])]
       (when-not (some #(= (:name %) "Grande Pirâmide") wps)
         (let [new-wps (conj wps {:name "Grande Pirâmide" :x cx :y (+ cy 1) :z (+ cz rad 3) :icon "🏛️"})]
           (spit wp-file (json/write-str new-wps {:indent true})))))
     [true (str "🏛️ Grande Pirâmide erguida com sucesso em [" cx ", " cy ", " cz "]!")])))

;; --- PURGE UNDERGROUND MONSTERS ---

(defn purge-underground-monsters! [player-name]
  (let [nbt (inv/read-player-nbt player-name)
        pos (if nbt (get nbt "Pos" [-146.0 107.0 -85.0]) [-146.0 107.0 -85.0])
        cx (int (Math/round (double (first pos))))
        cz (int (Math/round (double (nth pos 2))))
        safe-filter "type=!minecraft:player,type=!minecraft:iron_golem,type=!minecraft:allay,type=!minecraft:armadillo,type=!minecraft:cat,type=!minecraft:wolf,type=!minecraft:villager,type=!minecraft:cow,type=!minecraft:sheep,type=!minecraft:chicken,type=!minecraft:pig,type=!minecraft:horse,type=!minecraft:donkey,type=!minecraft:mule,type=!minecraft:llama,type=!minecraft:item_frame,type=!minecraft:glow_item_frame,type=!minecraft:armor_stand,type=!minecraft:painting,type=!minecraft:leash_knot,type=!minecraft:boat,type=!minecraft:chest_boat,type=!minecraft:minecart,type=!minecraft:chest_minecart"]
    (doseq [y [80 45 10 -25 -50]]
      (bridge/run-command! (str "execute positioned " cx " " y " " cz " run kill @e[" safe-filter ",distance=..65]")))
    (bridge/run-command! (str "execute positioned " cx " 20 " cz " run kill @e[type=minecraft:item,distance=..85]"))
    (bridge/run-command! (str "playsound minecraft:entity.experience_orb.pickup master @a " cx " 107 " cz " 1 1.2"))
    [true (str "🧹 Varredura subterrânea concluída sob [" cx ", " cz "]: Cavernas limpas e seguras!")]))

;; --- NUKE STRIKE ---

(defn trigger-nuke-strike! [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt (inv/read-player-nbt player-name)]
    (let [pos (get nbt "Pos" [0.0 0.0 0.0])
          px (double (first pos))
          py (double (second pos))
          pz (double (nth pos 2))
          dist-to-base (Math/hypot (- px -146.0) (- pz -85.0))]
      (if (< dist-to-base 40.0)
        [false (format "☢️ PERIGO CRÍTICO: Você está a apenas %dm da Base Principal! Afaste-se para pelo menos 40m para não vaporizar sua casa." (int dist-to-base))]
        (let [fast? (cfg/fast-timers?)]
          (future
            (try
              ;; Phase 1
              (bridge/run-command! "title @a times 5 35 10")
              (bridge/run-command! "title @a title {\"text\":\"☢️ ALERTA NUCLEAR ☢️\",\"color\":\"dark_red\",\"bold\":true}")
              (bridge/run-command! "title @a subtitle {\"text\":\"Míssil Balístico em Rota de Colisão...\",\"color\":\"gold\"}")
              (bridge/run-command! (str "playsound minecraft:block.bell.resonate master @a " (int px) " " (int py) " " (int pz) " 10 0.5"))
              (bridge/run-command! "effect give @a resistance 15 5 true")
              (bridge/run-command! "effect give @a fire_resistance 15 1 true")
              (Thread/sleep (if fast? 10 1400))
              ;; Phase 2
              (bridge/run-command! "title @a subtitle {\"text\":\"💥 IMPACTO IMINENTE EM 3... 2... 1...\",\"color\":\"red\",\"bold\":true}")
              (bridge/run-command! (str "playsound minecraft:entity.wither.spawn master @a " (int px) " " (int py) " " (int pz) " 8 0.7"))
              (bridge/run-command! (str "execute at " player-name " run particle flame ^ ^35 ^25 1 20 1 0.2 150"))
              (Thread/sleep (if fast? 10 1200))
              ;; Phase 3
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon lightning_bolt ~ ~ ~"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon lightning_bolt ~7 ~ ~7"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon lightning_bolt ~-7 ~ ~-7"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon lightning_bolt ~7 ~ ~-7"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon lightning_bolt ~-7 ~ ~7"))
              ;; Crater excavation
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-16 ~-2 ~-16 ~16 ~12 ~16 air replace"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-13 ~-5 ~-13 ~13 ~-1 ~13 air replace"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-10 ~-8 ~-10 ~10 ~-4 ~10 air replace"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-7 ~-11 ~-7 ~7 ~-7 ~7 air replace"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-4 ~-14 ~-4 ~4 ~-10 ~4 air replace"))
              ;; Scorched ground
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-8 ~-14 ~-8 ~8 ~-8 ~8 magma_block replace stone"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-10 ~-6 ~-10 ~10 ~-2 ~10 fire replace air"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-15 ~-3 ~-15 ~15 ~0 ~15 crying_obsidian replace grass_block"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run fill ~-15 ~-3 ~-15 ~15 ~0 ~15 crying_obsidian replace dirt"))
              ;; Creepers
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon creeper ~ ~-2 ~ {Fuse:0,ExplosionRadius:18b,ignited:1b}"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon creeper ~8 ~-3 ~8 {Fuse:0,ExplosionRadius:14b,ignited:1b}"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon creeper ~-8 ~-3 ~-8 {Fuse:0,ExplosionRadius:14b,ignited:1b}"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon creeper ~8 ~-3 ~-8 {Fuse:0,ExplosionRadius:14b,ignited:1b}"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run summon creeper ~-8 ~-3 ~8 {Fuse:0,ExplosionRadius:14b,ignited:1b}"))
              ;; Visuals
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run playsound minecraft:entity.generic.explode master @a ~ ~ ~ 10 0.4"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run playsound minecraft:entity.generic.explode master @a ~ ~ ~ 10 0.6"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run particle campfire_signal_smoke ~ ~ ~ 4 45 4 0.1 400"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run particle explosion_emitter ~ ~25 ~ 16 8 16 0.1 80"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run particle flame ~ ~28 ~ 20 6 20 0.15 350"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run particle lava ~ ~2 ~ 14 3 14 0.25 200"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run damage @e[type=!player,distance=..80] 99999 minecraft:magic"))
              (bridge/run-command! (str "execute at " player-name " positioned ^ ^ ^25 run damage @e[type=!player,distance=..80] 99999 minecraft:explosion"))
              (bridge/run-command! "effect give @a blindness 2 0 true")
              ;; Ground zero waypoint
              (let [rot (get nbt "Rotation" [0.0 0.0])
                    yaw (Math/toRadians (double (first rot)))
                    pitch (Math/toRadians (double (second rot)))
                    dx (* (- (Math/sin yaw)) (Math/cos pitch))
                    dy (- (Math/sin pitch))
                    dz (* (Math/cos yaw) (Math/cos pitch))
                    tx (Math/round (+ px (* dx 25.0)))
                    ty (Math/round (+ py (* dy 25.0)))
                    tz (Math/round (+ pz (* dz 25.0)))
                    wp-file (io/file (cfg/config-dir) "waypoints.json")
                    wps (if (.exists wp-file) (try (json/read-str (slurp wp-file) :key-fn keyword) (catch Exception _ [])) [])
                    clean-wps (remove #(str/starts-with? (str (:name %)) "☢️ Cratera") wps)
                    updated (conj clean-wps {:name "☢️ Cratera Ground Zero" :x tx :y ty :z tz :icon "☢️"})]
                (spit wp-file (json/write-str updated {:indent true})))
              (catch Exception _ nil)))
          [true "☢️ Míssil Balístico Nuclear disparado com sucesso! Impacto iminente a 25m na sua mira."])))
    [false (str "Jogador " player-name " não encontrado para calcular vetor de lançamento.")]))
