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

;; --- PREENCHIMENTO DE LAVA & DRENAGEM ---

(defn fill-lava!
  "Fills cavities (air/water) under/around the player with lava, preserving solid terrain, with safety lock and undo."
  ([player-name] (fill-lava! player-name 4 4))
  ([player-name radius depth]
   (bridge/run-command! "save-all")
   (if-let [nbt (inv/read-player-nbt player-name)]
     (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
           cx (int (Math/round (double (first pos))))
           cy (int (Math/round (double (second pos))))
           cz (int (Math/round (double (nth pos 2))))
           r (max 1 (min 15 (int (or radius 4))))
           d (max 1 (min 20 (int (or depth 4))))
           x1 (- cx r)
           x2 (+ cx r)
           z1 (- cz r)
           z2 (+ cz r)
           y-top (- cy 1)
           y-bot (- cy d)
           cmds
           [;; 1. Fire resistance buffer for safety
            (str "effect give " player-name " fire_resistance 30 1 true")
            ;; 2. Fill air cavities with lava
            (str "fill " x1 " " y-bot " " z1 " " x2 " " y-top " " z2 " lava replace air")
            ;; 3. Fill water cavities with lava
            (str "fill " x1 " " y-bot " " z1 " " x2 " " y-top " " z2 " lava replace water")
            ;; 4. Sound & effect
            (str "playsound minecraft:item.bucket.empty_lava master @a " cx " " cy " " cz " 1 1")]]
       (save-undo-state! :lava {:coords [x1 y-bot z1 x2 y-top z2]
                                :center [cx cy cz]
                                :player player-name})
       (doseq [c cmds] (bridge/run-command! c))
       [true (str "🌋 Cavidade [" x1 ".." x2 ", " y-bot ".." y-top ", " z1 ".." z2 "] preenchida com lava com sucesso!")])
     [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")])))

(defn undo-fill-lava!
  "Drains lava from the last filled volume by replacing lava and flowing_lava with air."
  []
  (if-let [state (:lava (get-undo-state))]
    (let [[x1 y-bot z1 x2 y-top z2] (:coords state)
          cmds [(str "fill " x1 " " y-bot " " z1 " " x2 " " y-top " " z2 " air replace lava")
                (str "fill " x1 " " y-bot " " z1 " " x2 " " y-top " " z2 " air replace flowing_lava")
                (str "playsound minecraft:item.bucket.fill_lava master @a " (first (:center state [0 0 0])) " " (second (:center state [0 0 0])) " " (nth (:center state [0 0 0]) 2) " 1 1")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :lava nil)
      [true (str "↩️ Lava drenada com sucesso na região [" x1 ".." x2 ", " y-bot ".." y-top "]!")])
    [false "Nenhum preenchimento de lava recente encontrado para desfazer."]))

;; --- CASTELO MEDIEVAL DE PEDRA ---

(defn build-stone-castle!
  "Erects an authentic 15x15 Medieval Stone Castle with 4 defensive towers, ramparts, battlements, gate, and throne."
  [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt (inv/read-player-nbt player-name)]
    (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
          cx (int (Math/round (double (first pos))))
          cy (int (Math/round (double (second pos))))
          cz (int (Math/round (double (nth pos 2))))
          rot (get nbt "Rotation" [0.0 0.0])
          cardinal (yaw->cardinal (first rot))
          
          ;; Castle footprint: 15x15 (radius 7 around player)
          hw 7
          min-x (- cx hw)
          max-x (+ cx hw)
          min-z (- cz hw)
          max-z (+ cz hw)
          
          cmds
          [;; 1. Clear construction envelope (15x15x12)
           (str "fill " min-x " " cy " " min-z " " max-x " " (+ cy 12) " " max-z " air replace")
           
           ;; 2. Solid foundation and courtyard floor
           (str "fill " (- min-x 1) " " (- cy 1) " " (- min-z 1) " " (+ max-x 1) " " (- cy 1) " " (+ max-z 1) " cobblestone replace")
           (str "fill " min-x " " (- cy 1) " " min-z " " max-x " " (- cy 1) " " max-z " stone_bricks replace")
           (str "fill " (+ min-x 1) " " (- cy 1) " " (+ min-z 1) " " (- max-x 1) " " (- cy 1) " " (- max-z 1) " polished_andesite replace")
           
           ;; 3. Outer curtain walls (cy to cy + 5)
           (str "fill " min-x " " cy " " min-z " " max-x " " (+ cy 5) " " max-z " stone_bricks replace")
           (str "fill " (+ min-x 1) " " cy " " (+ min-z 1) " " (- max-x 1) " " (+ cy 5) " " (- max-z 1) " air replace")
           
           ;; 4. Rampart walkway at cy + 4 around perimeter
           (str "fill " (+ min-x 1) " " (+ cy 4) " " (+ min-z 1) " " (- max-x 1) " " (+ cy 4) " " (- max-z 1) " stone_brick_slab[type=bottom] replace")
           (str "fill " (+ min-x 2) " " (+ cy 4) " " (+ min-z 2) " " (- max-x 2) " " (+ cy 4) " " (- max-z 2) " air replace")
           
           ;; 5. Outer wall battlements (cy + 6)
           (str "fill " min-x " " (+ cy 6) " " min-z " " max-x " " (+ cy 6) " " min-z " stone_brick_wall replace")
           (str "fill " min-x " " (+ cy 6) " " max-z " " max-x " " (+ cy 6) " " max-z " stone_brick_wall replace")
           (str "fill " min-x " " (+ cy 6) " " min-z " " min-x " " (+ cy 6) " " max-z " stone_brick_wall replace")
           (str "fill " max-x " " (+ cy 6) " " min-z " " max-x " " (+ cy 6) " " max-z " stone_brick_wall replace")
           
           ;; 6. Four Corner Defensive Towers (4x4, cy to cy + 8)
           ;; NW Tower
           (str "fill " min-x " " cy " " min-z " " (+ min-x 3) " " (+ cy 8) " " (+ min-z 3) " stone_bricks replace")
           (str "fill " (+ min-x 1) " " cy " " (+ min-z 1) " " (+ min-x 2) " " (+ cy 7) " " (+ min-z 2) " air replace")
           (str "fill " min-x " " (+ cy 9) " " min-z " " (+ min-x 3) " " (+ cy 9) " " (+ min-z 3) " stone_brick_wall replace")
           (str "setblock " (+ min-x 1) " " (+ cy 9) " " (+ min-z 1) " air replace")
           (str "setblock " (+ min-x 2) " " (+ cy 9) " " (+ min-z 1) " air replace")
           (str "setblock " (+ min-x 1) " " (+ cy 9) " " (+ min-z 2) " air replace")
           (str "setblock " (+ min-x 2) " " (+ cy 9) " " (+ min-z 2) " air replace")
           (str "setblock " min-x " " (+ cy 10) " " min-z " lantern[hanging=false] replace")
           (str "setblock " (+ min-x 3) " " (+ cy 10) " " min-z " lantern[hanging=false] replace")
           (str "setblock " min-x " " (+ cy 10) " " (+ min-z 3) " lantern[hanging=false] replace")
           (str "setblock " (+ min-x 3) " " (+ cy 10) " " (+ min-z 3) " lantern[hanging=false] replace")
           
           ;; NE Tower
           (str "fill " (- max-x 3) " " cy " " min-z " " max-x " " (+ cy 8) " " (+ min-z 3) " stone_bricks replace")
           (str "fill " (- max-x 2) " " cy " " (+ min-z 1) " " (- max-x 1) " " (+ cy 7) " " (+ min-z 2) " air replace")
           (str "fill " (- max-x 3) " " (+ cy 9) " " min-z " " max-x " " (+ cy 9) " " (+ min-z 3) " stone_brick_wall replace")
           (str "setblock " (- max-x 2) " " (+ cy 9) " " (+ min-z 1) " air replace")
           (str "setblock " (- max-x 1) " " (+ cy 9) " " (+ min-z 1) " air replace")
           (str "setblock " (- max-x 2) " " (+ cy 9) " " (+ min-z 2) " air replace")
           (str "setblock " (- max-x 1) " " (+ cy 9) " " (+ min-z 2) " air replace")
           (str "setblock " (- max-x 3) " " (+ cy 10) " " min-z " lantern[hanging=false] replace")
           (str "setblock " max-x " " (+ cy 10) " " min-z " lantern[hanging=false] replace")
           (str "setblock " (- max-x 3) " " (+ cy 10) " " (+ min-z 3) " lantern[hanging=false] replace")
           (str "setblock " max-x " " (+ cy 10) " " (+ min-z 3) " lantern[hanging=false] replace")
           
           ;; SW Tower
           (str "fill " min-x " " cy " " (- max-z 3) " " (+ min-x 3) " " (+ cy 8) " " max-z " stone_bricks replace")
           (str "fill " (+ min-x 1) " " cy " " (- max-z 2) " " (+ min-x 2) " " (+ cy 7) " " (- max-z 1) " air replace")
           (str "fill " min-x " " (+ cy 9) " " (- max-z 3) " " (+ min-x 3) " " (+ cy 9) " " max-z " stone_brick_wall replace")
           (str "setblock " (+ min-x 1) " " (+ cy 9) " " (- max-z 2) " air replace")
           (str "setblock " (+ min-x 2) " " (+ cy 9) " " (- max-z 2) " air replace")
           (str "setblock " (+ min-x 1) " " (+ cy 9) " " (- max-z 1) " air replace")
           (str "setblock " (+ min-x 2) " " (+ cy 9) " " (- max-z 1) " air replace")
           (str "setblock " min-x " " (+ cy 10) " " (- max-z 3) " lantern[hanging=false] replace")
           (str "setblock " (+ min-x 3) " " (+ cy 10) " " (- max-z 3) " lantern[hanging=false] replace")
           (str "setblock " min-x " " (+ cy 10) " " max-z " lantern[hanging=false] replace")
           (str "setblock " (+ min-x 3) " " (+ cy 10) " " max-z " lantern[hanging=false] replace")

           ;; SE Tower
           (str "fill " (- max-x 3) " " cy " " (- max-z 3) " " max-x " " (+ cy 8) " " max-z " stone_bricks replace")
           (str "fill " (- max-x 2) " " cy " " (- max-z 2) " " (- max-x 1) " " (+ cy 7) " " (- max-z 1) " air replace")
           (str "fill " (- max-x 3) " " (+ cy 9) " " (- max-z 3) " " max-x " " (+ cy 9) " " max-z " stone_brick_wall replace")
           (str "setblock " (- max-x 2) " " (+ cy 9) " " (- max-z 2) " air replace")
           (str "setblock " (- max-x 1) " " (+ cy 9) " " (- max-z 2) " air replace")
           (str "setblock " (- max-x 2) " " (+ cy 9) " " (- max-z 1) " air replace")
           (str "setblock " (- max-x 1) " " (+ cy 9) " " (- max-z 1) " air replace")
           (str "setblock " (- max-x 3) " " (+ cy 10) " " (- max-z 3) " lantern[hanging=false] replace")
           (str "setblock " max-x " " (+ cy 10) " " (- max-z 3) " lantern[hanging=false] replace")
           (str "setblock " (- max-x 3) " " (+ cy 10) " " (+ max-z 3) " lantern[hanging=false] replace")
           (str "setblock " max-x " " (+ cy 10) " " (+ max-z 3) " lantern[hanging=false] replace")

           ;; 7. Tower Access Ladder (in NW Tower)
           (str "fill " (+ min-x 2) " " cy " " (+ min-z 2) " " (+ min-x 2) " " (+ cy 8) " " (+ min-z 2) " ladder[facing=west] replace")
           (str "fill " (+ min-x 3) " " cy " " (+ min-z 1) " " (+ min-x 3) " " (+ cy 2) " " (+ min-z 1) " air replace")

           ;; 8. Main Gate Opening & Portcullis
           (case cardinal
             "north" (str "fill " (- cx 1) " " cy " " min-z " " (+ cx 1) " " (+ cy 3) " " min-z " air replace")
             "east"  (str "fill " max-x " " cy " " (- cz 1) " " max-x " " (+ cy 3) " " (+ cz 1) " air replace")
             "west"  (str "fill " min-x " " cy " " (- cz 1) " " min-x " " (+ cy 3) " " (+ cz 1) " air replace")
             (str "fill " (- cx 1) " " cy " " max-z " " (+ cx 1) " " (+ cy 3) " " max-z " air replace"))
           
           (case cardinal
             "north" (str "fill " (- cx 1) " " (+ cy 3) " " min-z " " (+ cx 1) " " (+ cy 3) " " min-z " iron_bars replace")
             "east"  (str "fill " max-x " " (+ cy 3) " " (- cz 1) " " max-x " " (+ cy 3) " " (+ cz 1) " iron_bars replace")
             "west"  (str "fill " min-x " " (+ cy 3) " " (- cz 1) " " min-x " " (+ cy 3) " " (+ cz 1) " iron_bars replace")
             (str "fill " (- cx 1) " " (+ cy 3) " " max-z " " (+ cx 1) " " (+ cy 3) " " max-z " iron_bars replace"))

           ;; 9. Central Courtyard: Medieval Throne, Red Carpet & Arsenal
           (str "setblock " cx " " cy " " (- cz 3) " chiseled_stone_bricks replace")
           (str "setblock " cx " " (+ cy 1) " " (- cz 3) " stone_brick_stairs[facing=south,half=bottom] replace")
           (str "setblock " cx " " (+ cy 2) " " (- cz 3) " red_banner replace")
           (str "setblock " (- cx 1) " " cy " " (- cz 3) " stone_brick_stairs[facing=east,half=bottom] replace")
           (str "setblock " (+ cx 1) " " cy " " (- cz 3) " stone_brick_stairs[facing=west,half=bottom] replace")
           (str "fill " cx " " cy " " (- cz 2) " " cx " " cy " " (+ cz 4) " red_carpet replace")
           (str "setblock " (- cx 3) " " cy " " (- cz 1) " anvil replace")
           (str "setblock " (- cx 3) " " cy " " cz " crafting_table replace")
           (str "setblock " (- cx 3) " " cy " " (+ cz 1) " blast_furnace replace")
           (str "setblock " (+ cx 3) " " cy " " (- cz 1) " stonecutter replace")
           (str "setblock " (+ cx 3) " " cy " " cz " chest replace")
           (str "setblock " (+ cx 3) " " cy " " (+ cz 1) " barrel replace")
           
           ;; Courtyard Torches & Lanterns
           (str "setblock " (- cx 2) " " (+ cy 2) " " (- cz 2) " lantern[hanging=false] replace")
           (str "setblock " (+ cx 2) " " (+ cy 2) " " (- cz 2) " lantern[hanging=false] replace")
           (str "setblock " (- cx 2) " " (+ cy 2) " " (+ cz 2) " lantern[hanging=false] replace")
           (str "setblock " (+ cx 2) " " (+ cy 2) " " (+ cz 2) " lantern[hanging=false] replace")

           ;; 10. Safe player placement & sound
           (str "tp " player-name " " cx " " cy " " cz)
           (str "playsound minecraft:ui.toast.challenge_complete master @a " cx " " cy " " cz " 1 1")]]
      
      (save-undo-state! :stone-castle {:coords [cx cy cz]
                                       :bounds [min-x max-x min-z max-z]
                                       :cy cy
                                       :player player-name})
      (doseq [c cmds] (bridge/run-command! c))
      [true (str "🏰 Castelo Medieval de Pedra (15x15) erguido com sucesso em [" cx ", " cy ", " cz "]!")])
    [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")]))

(defn undo-stone-castle!
  "Demolishes the last stone castle and restores natural ground with grass."
  []
  (if-let [state (:stone-castle (get-undo-state))]
    (let [[cx cy cz] (:coords state)
          [min-x max-x min-z max-z] (:bounds state)
          cmds [(str "fill " (- min-x 1) " " (- cy 1) " " (- min-z 1) " " (+ max-x 1) " " (+ cy 12) " " (+ max-z 1) " air replace")
                (str "fill " (- min-x 1) " " (- cy 1) " " (- min-z 1) " " (+ max-x 1) " " (- cy 1) " " (+ max-z 1) " grass_block replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :stone-castle nil)
      [true (str "↩️ Castelo Medieval de pedra em [" cx ", " cy ", " cz "] desfeito e terreno restaurado com grama!")])
    [false "Nenhum castelo recente encontrado no histórico para desfazer."]))

;; --- CASTELO TITÂNICO DO VAZIO (A CIDADELA QWEN) ---

(defn build-titanic-castle!
  "Erects the Titanic Void Citadel (41x41): a colossal double-walled keep built over a moat, with four
   colossal corner towers sporting conical roofs and beacon pinnacles, a massive central donjon topped by
   a spire, battlemented ramparts, a drawbridge gatehouse, a stained-glass chapel and inner gardens."
  [player-name]
  (bridge/run-command! "save-all")
  (if-let [nbt (inv/read-player-nbt player-name)]
    (let [pos (get nbt "Pos" [-146.0 107.0 -85.0])
          cx (int (Math/round (double (first pos))))
          cy (int (Math/round (double (second pos))))
          cz (int (Math/round (double (nth pos 2))))
          rot (get nbt "Rotation" [0.0 0.0])
          cardinal (yaw->cardinal (first rot))

          ;; Footprint: 41x41 (radius 20). Outer curtain at ±20, inner curtain at ±16.
          B 20
          I 16
          wx1 (- cx B) wx2 (+ cx B)
          wz1 (- cz B) wz2 (+ cz B)
          ix1 (- cx I) ix2 (+ cx I)
          iz1 (- cz I) iz2 (+ cz I)

          ;; Ground/clearance levels
          clear-top (+ cy 40)
          base (- cy 1)

          tower-top (+ cy 18)

          cmds
          [;; ── 1. GROUND PREP: clear the envelope and lay the surrounding courtyard ──
           (str "fill " (- wx1 6) " " cy " " (- wz1 6) " " (+ wx2 6) " " clear-top " " (+ wz2 6) " air replace")
           (str "fill " (- wx1 8) " " base " " (- wz1 8) " " (+ wx2 8) " " base " " (+ wz2 8) " grass_block replace")
           (str "fill " (- wx1 4) " " base " " (- wz1 4) " " (+ wx2 4) " " base " " (+ wz2 4) " polished_andesite replace")

           ;; ── 2. MOAT: dig a 3-wide trench ringing the walls and flood it ──
           (str "fill " (- wx1 3) " " (- cy 3) " " (- wz1 3) " " (+ wx2 3) " " base " " (+ wz2 3) " water replace")
           (str "fill " (- wx1 2) " " (- cy 3) " " (- wz1 2) " " (+ wx2 2) " " base " " (+ wz2 2) " air replace")

           ;; ── 3. STONE BASE PLINTH under the whole citadel ──
           (str "fill " wx1 " " base " " wz1 " " wx2 " " base " " wz2 " stone_bricks replace")
           (str "fill " (- wx1 1) " " (- cy 2) " " (- wz1 1) " " (+ wx2 1) " " (- cy 2) " " (+ wz2 1) " cobblestone replace")

           ;; ── 4. OUTER CURTAIN WALL (5 high) with inner hollow ──
           (str "fill " wx1 " " cy " " wz1 " " wx2 " " (+ cy 5) " " wz2 " stone_bricks replace")
           (str "fill " (+ wx1 1) " " cy " " (+ wz1 1) " " (- wx2 1) " " (+ cy 5) " " (- wz2 1) " air replace")

           ;; ── 5. INNER CURTAIN WALL (7 high), 4m behind the outer ──
           (str "fill " ix1 " " cy " " iz1 " " ix2 " " (+ cy 7) " " iz2 " deepslate_bricks replace")
           (str "fill " (+ ix1 1) " " cy " " (+ iz1 1) " " (- ix2 1) " " (+ cy 7) " " (- iz2 1) " air replace")

           ;; ── 6. CRENELLATIONS on both curtains ──
           (str "fill " wx1 " " (+ cy 6) " " wz1 " " wx2 " " (+ cy 6) " " wz1 " stone_brick_wall replace")
           (str "fill " wx1 " " (+ cy 6) " " wz2 " " wx2 " " (+ cy 6) " " wz2 " stone_brick_wall replace")
           (str "fill " wx1 " " (+ cy 6) " " wz1 " " wx1 " " (+ cy 6) " " wz2 " stone_brick_wall replace")
           (str "fill " wx2 " " (+ cy 6) " " wz1 " " wx2 " " (+ cy 6) " " wz2 " stone_brick_wall replace")
           (str "fill " ix1 " " (+ cy 8) " " iz1 " " ix2 " " (+ cy 8) " " iz1 " deepslate_tile_wall replace")
           (str "fill " ix1 " " (+ cy 8) " " iz2 " " ix2 " " (+ cy 8) " " iz2 " deepslate_tile_wall replace")
           (str "fill " ix1 " " (+ cy 8) " " iz1 " " ix1 " " (+ cy 8) " " iz2 " deepslate_tile_wall replace")
           (str "fill " ix2 " " (+ cy 8) " " iz1 " " ix2 " " (+ cy 8) " " iz2 " deepslate_tile_wall replace")

           ;; ── 7. RAMPART WALKWAY between the two curtains (at cy+4) ──
           (str "fill " wx1 " " (+ cy 4) " " wz1 " " ix2 " " (+ cy 4) " " iz2 " stone_brick_slab[type=top] replace")
           (str "fill " (+ wx1 1) " " (+ cy 4) " " (+ wz1 1) " " (- ix2 1) " " (+ cy 4) " " (- iz2 1) " air replace")

           ;; ── 8. FOUR COLOSSAL CORNER TOWERS (7x7, cy → cy+18, conical roofs) ──
           ;; NW
           (str "fill " wx1 " " cy " " wz1 " " (+ wx1 6) " " tower-top " " (+ wz1 6) " stone_bricks replace")
           (str "fill " (+ wx1 1) " " cy " " (+ wz1 1) " " (+ wx1 5) " " (- tower-top 3) " " (+ wz1 5) " air replace")
           (str "fill " (+ wx1 2) " " (+ cy 2) " " (+ wz1 2) " " (+ wx1 4) " " (+ cy 8) " " (+ wz1 4) " air replace")
           (str "fill " (+ wx1 1) " " (- tower-top 2) " " (+ wz1 1) " " (+ wx1 5) " " (- tower-top 2) " " (+ wz1 5) " stone_brick_slab[type=bottom] replace")
           (str "fill " wx1 " " tower-top " " wz1 " " (+ wx1 6) " " (+ tower-top 2) " " (+ wz1 6) " air replace")
           (str "fill " (+ wx1 1) " " (+ tower-top 1) " " (+ wz1 1) " " (+ wx1 5) " " (+ tower-top 1) " " (+ wz1 5) " stone_bricks replace")
           (str "fill " (+ wx1 2) " " (+ tower-top 2) " " (+ wz1 2) " " (+ wx1 4) " " (+ tower-top 2) " " (+ wz1 4) " stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 3) " " (+ wz1 3) " stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 4) " " (+ wz1 3) " chiseled_stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 5) " " (+ wz1 3) " beacon replace")
           ;; SE
           (str "fill " (- wx2 6) " " cy " " (- wz2 6) " " wx2 " " tower-top " " wz2 " stone_bricks replace")
           (str "fill " (- wx2 5) " " cy " " (- wz2 5) " " (- wx2 1) " " (- tower-top 3) " " (- wz2 1) " air replace")
           (str "fill " (- wx2 4) " " (+ cy 2) " " (- wz2 4) " " (- wx2 2) " " (+ cy 8) " " (- wz2 2) " air replace")
           (str "fill " (- wx2 5) " " (- tower-top 2) " " (- wz2 5) " " (- wx2 1) " " (- tower-top 2) " " (- wz2 1) " stone_brick_slab[type=bottom] replace")
           (str "fill " (- wx2 6) " " tower-top " " (- wz2 6) " " wx2 " " (+ tower-top 2) " " wz2 " air replace")
           (str "fill " (- wx2 5) " " (+ tower-top 1) " " (- wz2 5) " " (- wx2 1) " " (+ tower-top 1) " " (- wz2 1) " stone_bricks replace")
           (str "fill " (- wx2 4) " " (+ tower-top 2) " " (- wz2 4) " " (- wx2 2) " " (+ tower-top 2) " " (- wz2 2) " stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 3) " " (- wz2 3) " stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 4) " " (- wz2 3) " chiseled_stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 5) " " (- wz2 3) " beacon replace")
           ;; NE
           (str "fill " (- wx2 6) " " cy " " wz1 " " wx2 " " tower-top " " (+ wz1 6) " stone_bricks replace")
           (str "fill " (- wx2 5) " " cy " " (+ wz1 1) " " (- wx2 1) " " (- tower-top 3) " " (+ wz1 5) " air replace")
           (str "fill " (- wx2 4) " " (+ cy 2) " " (+ wz1 2) " " (- wx2 2) " " (+ cy 8) " " (+ wz1 4) " air replace")
           (str "fill " (- wx2 5) " " (- tower-top 2) " " (+ wz1 1) " " (- wx2 1) " " (- tower-top 2) " " (+ wz1 5) " stone_brick_slab[type=bottom] replace")
           (str "fill " (- wx2 6) " " tower-top " " wz1 " " wx2 " " (+ tower-top 2) " " (+ wz1 6) " air replace")
           (str "fill " (- wx2 5) " " (+ tower-top 1) " " (+ wz1 1) " " (- wx2 1) " " (+ tower-top 1) " " (+ wz1 5) " stone_bricks replace")
           (str "fill " (- wx2 4) " " (+ tower-top 2) " " (+ wz1 2) " " (- wx2 2) " " (+ tower-top 2) " " (+ wz1 4) " stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 3) " " (+ wz1 3) " stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 4) " " (+ wz1 3) " chiseled_stone_bricks replace")
           (str "setblock " (- wx2 3) " " (+ tower-top 5) " " (+ wz1 3) " beacon replace")
           ;; SW
           (str "fill " wx1 " " cy " " (- wz2 6) " " (+ wx1 6) " " tower-top " " wz2 " stone_bricks replace")
           (str "fill " (+ wx1 1) " " cy " " (- wz2 5) " " (+ wx1 5) " " (- tower-top 3) " " (- wz2 1) " air replace")
           (str "fill " (+ wx1 2) " " (+ cy 2) " " (- wz2 4) " " (+ wx1 4) " " (+ cy 8) " " (- wz2 2) " air replace")
           (str "fill " (+ wx1 1) " " (- tower-top 2) " " (- wz2 5) " " (+ wx1 5) " " (- tower-top 2) " " (- wz2 1) " stone_brick_slab[type=bottom] replace")
           (str "fill " wx1 " " tower-top " " (- wz2 6) " " (+ wx1 6) " " (+ tower-top 2) " " wz2 " air replace")
           (str "fill " (+ wx1 1) " " (+ tower-top 1) " " (- wz2 5) " " (+ wx1 5) " " (+ tower-top 1) " " (- wz2 1) " stone_bricks replace")
           (str "fill " (+ wx1 2) " " (+ tower-top 2) " " (- wz2 4) " " (+ wx1 4) " " (+ tower-top 2) " " (- wz2 2) " stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 3) " " (- wz2 3) " stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 4) " " (- wz2 3) " chiseled_stone_bricks replace")
           (str "setblock " (+ wx1 3) " " (+ tower-top 5) " " (- wz2 3) " beacon replace")

           ;; ── 9. CENTRAL DONJON (11x11 keep, cy → cy+22) crowned by a spire ──
           (str "fill " (- cx 5) " " cy " " (- cz 5) " " (+ cx 5) " " (+ cy 22) " " (+ cz 5) " stone_bricks replace")
           (str "fill " (- cx 4) " " cy " " (- cz 4) " " (+ cx 4) " " (+ cy 20) " " (+ cz 4) " air replace")
           (str "fill " (- cx 5) " " (+ cy 22) " " (- cz 5) " " (+ cx 5) " " (+ cy 22) " " (+ cz 5) " stone_brick_slab[type=top] replace")
           ;; Crown battlements
           (str "fill " (- cx 5) " " (+ cy 23) " " (- cz 5) " " (+ cx 5) " " (+ cy 23) " " (- cz 5) " stone_brick_wall replace")
           (str "fill " (- cx 5) " " (+ cy 23) " " (+ cz 5) " " (+ cx 5) " " (+ cy 23) " " (+ cz 5) " stone_brick_wall replace")
           (str "fill " (- cx 5) " " (+ cy 23) " " (- cz 5) " " (- cx 5) " " (+ cy 23) " " (+ cz 5) " stone_brick_wall replace")
           (str "fill " (+ cx 5) " " (+ cy 23) " " (- cz 5) " " (+ cx 5) " " (+ cy 23) " " (+ cz 5) " stone_brick_wall replace")
           ;; Spire: tapering roof + gold finial + beacon
           (str "fill " (- cx 4) " " (+ cy 23) " " (- cz 4) " " (+ cx 4) " " (+ cy 23) " " (+ cz 4) " deepslate_tiles replace")
           (str "fill " (- cx 3) " " (+ cy 24) " " (- cz 3) " " (+ cx 3) " " (+ cy 24) " " (+ cz 3) " deepslate_tiles replace")
           (str "fill " (- cx 2) " " (+ cy 25) " " (- cz 2) " " (+ cx 2) " " (+ cy 25) " " (+ cz 2) " deepslate_tiles replace")
           (str "fill " (- cx 1) " " (+ cy 26) " " (- cz 1) " " (+ cx 1) " " (+ cy 26) " " (+ cz 1) " deepslate_tiles replace")
           (str "setblock " cx " " (+ cy 27) " " cz " deepslate_tiles replace")
           (str "setblock " cx " " (+ cy 28) " " cz " gold_block replace")
           (str "setblock " cx " " (+ cy 29) " " cz " beacon replace")
           ;; Grand arched gateway into the donjon (facing player's cardinal)
           (case cardinal
             "north" (str "fill " (- cx 1) " " cy " " (- cz 5) " " (+ cx 1) " " (+ cy 4) " " (- cz 5) " air replace")
             "east"  (str "fill " (+ cx 5) " " cy " " (- cz 1) " " (+ cx 5) " " (+ cy 4) " " (+ cz 1) " air replace")
             "west"  (str "fill " (- cx 5) " " cy " " (- cz 1) " " (- cx 5) " " (+ cy 4) " " (+ cz 1) " air replace")
             (str "fill " (- cx 1) " " cy " " (+ cz 5) " " (+ cx 1) " " (+ cy 4) " " (+ cz 5) " air replace"))

           ;; ── 10. MAIN GATEHOUSE + DRAWBRIDGE (aligned to player facing) ──
           (case cardinal
             "north" (str "fill " (- cx 2) " " cy " " wx1 " " (+ cx 2) " " (+ cy 5) " " wx1 " air replace")
             "east"  (str "fill " wx2 " " cy " " (- cz 2) " " wx2 " " (+ cy 5) " " (+ cz 2) " air replace")
             "west"  (str "fill " wx1 " " cy " " (- cz 2) " " wx1 " " (+ cy 5) " " (+ cz 2) " air replace")
             (str "fill " (- cx 2) " " cy " " wz2 " " (+ cx 2) " " (+ cy 5) " " wz2 " air replace"))
           (case cardinal
             "north" (str "fill " (- cx 2) " " cy " " ix1 " " (+ cx 2) " " (+ cy 5) " " ix1 " air replace")
             "east"  (str "fill " ix2 " " cy " " (- cz 2) " " ix2 " " (+ cy 5) " " (+ cz 2) " air replace")
             "west"  (str "fill " ix1 " " cy " " (- cz 2) " " ix1 " " (+ cy 5) " " (+ cz 2) " air replace")
             (str "fill " (- cx 2) " " cy " " iz2 " " (+ cx 2) " " (+ cy 5) " " iz2 " air replace"))
           ;; Portcullis bars overhead
           (case cardinal
             "north" (str "fill " (- cx 2) " " (+ cy 4) " " wx1 " " (+ cx 2) " " (+ cy 4) " " wx1 " iron_bars replace")
             "east"  (str "fill " wx2 " " (+ cy 4) " " (- cz 2) " " wx2 " " (+ cy 4) " " (+ cz 2) " iron_bars replace")
             "west"  (str "fill " wx1 " " (+ cy 4) " " (- cz 2) " " wx1 " " (+ cy 4) " " (+ cz 2) " iron_bars replace")
             (str "fill " (- cx 2) " " (+ cy 4) " " wz2 " " (+ cx 2) " " (+ cy 4) " " wz2 " iron_bars replace"))
           ;; Drawbridge: oak planks spanning the moat + rails
           (case cardinal
             "north" (str "fill " (- cx 2) " " base " " (- wz1 5) " " (+ cx 2) " " base " " (- wz1 1) " oak_planks replace")
             "east"  (str "fill " (+ wx2 1) " " base " " (- cz 2) " " (+ wx2 5) " " base " " (+ cz 2) " oak_planks replace")
             "west"  (str "fill " (- wx1 5) " " base " " (- cz 2) " " (- wx1 1) " " base " " (+ cz 2) " oak_planks replace")
             (str "fill " (- cx 2) " " base " " (+ wz1 1) " " (+ cx 2) " " base " " (+ wz1 5) " oak_planks replace"))

           ;; ── 11. CHAPEL (stained glass) in the west courtyard ──
           (str "fill " (+ ix1 3) " " cy " " (- cz 3) " " (+ ix1 8) " " (+ cy 6) " " (+ cz 3) " deepslate_bricks replace")
           (str "fill " (+ ix1 4) " " cy " " (- cz 2) " " (+ ix1 7) " " (+ cy 5) " " (+ cz 2) " air replace")
           (str "fill " (+ ix1 3) " " (+ cy 1) " " (- cz 2) " " (+ ix1 3) " " (+ cy 4) " " (- cz 2) " cyan_stained_glass replace")
           (str "fill " (+ ix1 3) " " (+ cy 1) " " cz " " (+ ix1 3) " " (+ cy 4) " " cz " magenta_stained_glass replace")
           (str "fill " (+ ix1 3) " " (+ cy 1) " " (+ cz 2) " " (+ ix1 3) " " (+ cy 4) " " (+ cz 2) " cyan_stained_glass replace")
           (str "fill " (+ ix1 3) " " (+ cy 7) " " (- cz 3) " " (+ ix1 8) " " (+ cy 7) " " (+ cz 3) " deepslate_tiles replace")
           (str "fill " (+ ix1 4) " " (+ cy 7) " " (- cz 2) " " (+ ix1 7) " " (+ cy 7) " " (+ cz 2) " deepslate_tile_slab[type=top] replace")
           (str "setblock " (+ ix1 6) " " (+ cy 8) " " cz " end_rod replace")

           ;; ── 12. INNER GARDENS & WATER WORKS (east courtyard) ──
           (str "fill " (- ix2 7) " " base " " (- cz 3) " " (- ix2 2) " " base " " (+ cz 3) " grass_block replace")
           (str "fill " (- ix2 6) " " base " " (- cz 2) " " (- ix2 3) " " base " " (+ cz 2) " water replace")
           (str "setblock " (- ix2 5) " " cy " " cz " lily_pad replace")
           (str "setblock " (- ix2 4) " " cy " " (- cz 1) " lily_pad replace")
           (str "setblock " (- ix2 7) " " cy " " cz " oak_log replace")
           (str "setblock " (- ix2 7) " " (+ cy 1) " " cz " oak_leaves[persistent=true] replace")
           (str "setblock " (- ix2 6) " " cy " " (+ cz 3) " sweet_berry_bush replace")
           (str "setblock " (- ix2 4) " " cy " " (+ cz 3) " sweet_berry_bush replace")
           (str "setblock " (- ix2 5) " " cy " " (- cz 3) " azure_bluet replace")
           (str "setblock " (- ix2 4) " " cy " " (- cz 3) " oxeye_daisy replace")

           ;; ── 13. THRONE HALL, BANNERS & LANTERNS inside the donjon ──
           (str "fill " (- cx 4) " " cy " " (- cz 4) " " (+ cx 4) " " cy " " (+ cz 4) " polished_deepslate replace")
           (str "fill " cx " " cy " " (- cz 4) " " cx " " cy " " (+ cz 4) " red_carpet replace")
           (str "setblock " cx " " cy " " (- cz 4) " chiseled_stone_bricks replace")
           (str "setblock " cx " " (+ cy 1) " " (- cz 4) " stone_brick_stairs[facing=south,half=bottom] replace")
           (str "setblock " cx " " (+ cy 2) " " (- cz 4) " red_banner replace")
           (str "setblock " (- cx 1) " " cy " " (- cz 4) " stone_brick_stairs[facing=east,half=bottom] replace")
           (str "setblock " (+ cx 1) " " cy " " (- cz 4) " stone_brick_stairs[facing=west,half=bottom] replace")
           (str "setblock " (- cx 4) " " (+ cy 3) " " (- cz 4) " red_banner replace")
           (str "setblock " (+ cx 4) " " (+ cy 3) " " (- cz 4) " red_banner replace")
           (str "setblock " (- cx 3) " " cy " " (- cz 2) " anvil replace")
           (str "setblock " (- cx 3) " " cy " " (- cz 1) " crafting_table replace")
           (str "setblock " (- cx 3) " " cy " " cz " blast_furnace replace")
           (str "setblock " (- cx 3) " " cy " " (+ cz 1) " smithing_table replace")
           (str "setblock " (+ cx 3) " " cy " " (- cz 2) " chest replace")
           (str "setblock " (+ cx 3) " " cy " " (- cz 1) " barrel replace")
           (str "setblock " (+ cx 3) " " cy " " cz " stonecutter replace")
           (str "setblock " (+ cx 3) " " cy " " (+ cz 1) " ender_chest replace")
           (str "setblock " (- cx 3) " " (+ cy 3) " " cz " lantern[hanging=true] replace")
           (str "setblock " (+ cx 3) " " (+ cy 3) " " cz " lantern[hanging=true] replace")
           (str "setblock " cx " " (+ cy 3) " " (- cz 1) " lantern[hanging=true] replace")
           (str "setblock " cx " " (+ cy 3) " " (+ cz 1) " lantern[hanging=true] replace")

           ;; ── 14. RAMPART LANTERNS & TORCHES around the ring ──
           (str "setblock " (+ ix1 2) " " (+ cy 5) " " iz1 " lantern[hanging=false] replace")
           (str "setblock " (- ix2 2) " " (+ cy 5) " " iz1 " lantern[hanging=false] replace")
           (str "setblock " (+ ix1 2) " " (+ cy 5) " " iz2 " lantern[hanging=false] replace")
           (str "setblock " (- ix2 2) " " (+ cy 5) " " iz2 " lantern[hanging=false] replace")
           (str "setblock " ix1 " " (+ cy 5) " " (+ iz1 2) " lantern[hanging=false] replace")
           (str "setblock " ix1 " " (+ cy 5) " " (- iz2 2) " lantern[hanging=false] replace")
           (str "setblock " ix2 " " (+ cy 5) " " (+ iz1 2) " lantern[hanging=false] replace")
           (str "setblock " ix2 " " (+ cy 5) " " (- iz2 2) " lantern[hanging=false] replace")

           ;; ── 15. STONE STAIRCASE UP TO THE RAMPARTS ──
           (str "fill " (+ cx 2) " " cy " " (+ cz 9) " " (+ cx 4) " " cy " " (+ cz 9) " polished_andesite replace")
           (str "setblock " (+ cx 2) " " cy " " (+ cz 9) " stone_brick_stairs[facing=north,half=bottom] replace")
           (str "setblock " (+ cx 3) " " (+ cy 1) " " (+ cz 9) " stone_brick_stairs[facing=north,half=bottom] replace")
           (str "setblock " (+ cx 4) " " (+ cy 2) " " (+ cz 9) " stone_brick_stairs[facing=north,half=bottom] replace")
           (str "setblock " (+ cx 4) " " (+ cy 3) " " (+ cz 9) " stone_brick_stairs[facing=north,half=bottom] replace")
           (str "fill " (+ cx 2) " " (+ cy 4) " " (+ cz 9) " " (+ cx 4) " " (+ cy 4) " " (+ cz 9) " stone_brick_slab[type=bottom] replace")

           ;; ── 16. SOUND, SAFE PLACEMENT ON THE KEEP CROWN ──
           (str "tp " player-name " " cx " " (+ cy 23) " " cz)
           (str "playsound minecraft:ui.toast.challenge_complete master @a " cx " " cy " " cz " 1 1")
           (str "playsound minecraft:block.beacon.activate master @a " cx " " (+ cy 29) " " cz " 1 1")]]

      (save-undo-state! :titanic-castle {:coords [cx cy cz]
                                         :bounds [wx1 wx2 wz1 wz2]
                                         :top (+ cy 30)
                                         :player player-name})
      (doseq [c cmds] (bridge/run-command! c))
      [true (str "🌌 Castelo Titânico do Vazio (41x41) erguido em [" cx ", " cy ", " cz "] — muralha dupla, fosso, 4 torres colossais, torreão com pináculo e farol!")])
    [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")]))

(defn undo-titanic-castle!
  "Demolishes the Titanic Void Citadel, drains its moat and restores natural grass terrain."
  []
  (if-let [state (:titanic-castle (get-undo-state))]
    (let [[cx cy cz] (:coords state)
          [wx1 wx2 wz1 wz2] (:bounds state)
          top (get state :top (+ cy 30))
          cmds [(str "fill " (- wx1 8) " " (- cy 4) " " (- wz1 8) " " (+ wx2 8) " " top " " (+ wz2 8) " air replace")
                (str "fill " (- wx1 8) " " (- cy 3) " " (- wz1 8) " " (+ wx2 8) " " (- cy 3) " " (+ wz2 8) " air replace")
                (str "fill " (- wx1 8) " " (- cy 2) " " (- wz1 8) " " (+ wx2 8) " " (- cy 2) " " (+ wz2 8) " air replace")
                (str "fill " (- wx1 1) " " (- cy 1) " " (- wz1 1) " " (+ wx2 1) " " (- cy 1) " " (+ wz2 1) " grass_block replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (save-undo-state! :titanic-castle nil)
      [true (str "↩️ Castelo Titânico do Vazio em [" cx ", " cy ", " cz "] desfeito, fosso drenado e terreno restaurado com grama!")])
    [false "Nenhum Castelo Titânico recente encontrado no histórico para desfazer."]))
