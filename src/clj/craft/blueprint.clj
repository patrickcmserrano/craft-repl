(ns craft.blueprint
  "3D Structure Blueprint scanner, Mojang Structure NBT serializer, template placement, and vertically sliced undo."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [craft.bridge :as bridge]
            [craft.builds :as builds]
            [craft.config :as cfg]
            [craft.inventory :as inv]
            [craft.nbt :as nbt])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream File]
           [java.nio ByteBuffer ByteOrder]
           [java.text SimpleDateFormat]
           [java.util Date]
           [java.util.zip InflaterInputStream GZIPOutputStream]))

(def min-x -164)
(def max-x -127)
(def min-y 99)
(def max-y 118)
(def min-z -112)
(def max-z -65)

(def size-x (inc (- max-x min-x)))
(def size-y (inc (- max-y min-y)))
(def size-z (inc (- max-z min-z)))

(defn- unpack-section [sec]
  (let [bs (get sec "block_states" {})
        palette (get bs "palette" [])
        data (get bs "data" [])]
    (cond
      (empty? palette) {}
      (or (= (count palette) 1) (empty? data))
      (let [raw-p (first palette)]
        (into {} (for [x (range 16) y (range 16) z (range 16)] [[x y z] raw-p])))
      :else
      (let [bits (max 4 (int (Math/ceil (/ (Math/log (count palette)) (Math/log 2.0)))))
            blocks-per-long (quot 64 bits)
            mask (dec (bit-shift-left 1 bits))
            res (atom {})
            block-idx (atom 0)]
        (doseq [long-val data]
          (let [u-val (if (neg? (long long-val)) (+ (BigInteger/valueOf (long long-val)) (.shiftLeft BigInteger/ONE 64)) (BigInteger/valueOf (long long-val)))]
            (dotimes [i blocks-per-long]
              (when (< @block-idx 4096)
                (let [p-idx (.intValue (.and (.shiftRight ^BigInteger u-val (* i bits)) (BigInteger/valueOf mask)))]
                  (when (< p-idx (count palette))
                    (let [bx (mod @block-idx 16)
                          bz (mod (quot @block-idx 16) 16)
                          by (quot @block-idx 256)]
                      (swap! res assoc [bx by bz] (nth palette p-idx))))
                  (swap! block-idx inc))))))
        @res))))

(defn scan-world-volume [^File mca-file]
  (let [world-blocks (atom {})
        world-entities (atom {})]
    (when (.exists mca-file)
      (let [min-cx (quot min-x 16)
            max-cx (quot max-x 16)
            min-cz (quot min-z 16)
            max-cz (quot max-z 16)]
        (doseq [cx (range min-cx (inc max-cx))
                cz (range min-cz (inc max-cz))]
          (when-let [chunk (nbt/read-anvil-chunk-nbt mca-file cx cz)]
            ;; Block entities
            (doseq [be (get chunk "block_entities" [])]
              (let [bx (get be "x")
                    by (get be "y")
                    bz (get be "z")]
                (when (and bx by bz
                           (<= min-x bx max-x)
                           (<= min-y by max-y)
                           (<= min-z bz max-z))
                  (swap! world-entities assoc [bx by bz] be))))
            ;; Sections
            (doseq [sec (get chunk "sections" [])]
              (let [sec-y (get sec "Y")
                    blocks (unpack-section sec)]
                (doseq [[[bx by bz] p-dict] blocks]
                  (let [gx (+ (* cx 16) bx)
                        gy (+ (* sec-y 16) by)
                        gz (+ (* cz 16) bz)]
                    (when (and (<= min-x gx max-x)
                               (<= min-y gy max-y)
                               (<= min-z gz max-z))
                      (swap! world-blocks assoc [gx gy gz] p-dict))))))))))
    [@world-blocks @world-entities]))

(defn backup-main-base! []
  (bridge/run-command! "save-all flush")
  (let [[world-blocks world-entities] (scan-world-volume (cfg/mca-path))
        palette (atom [])
        palette-map (atom {})
        get-palette-idx (fn [b-dict]
                          (let [bname (get b-dict "Name" "minecraft:air")
                                props (get b-dict "Properties" {})
                                key [bname props]]
                            (if-let [idx (get @palette-map key)]
                              idx
                              (let [new-idx (count @palette)]
                                (swap! palette-map assoc key new-idx)
                                (swap! palette conj {"Name" bname "Properties" props})
                                new-idx))))
        blocks-list (atom [])
        non-air-count (atom 0)
        counts-by-block (atom {})]
    (doseq [y (range min-y (inc max-y))
            z (range min-z (inc max-z))
            x (range min-x (inc max-x))]
      (let [b-dict (get world-blocks [x y z] {"Name" "minecraft:air"})
            bname (get b-dict "Name" "minecraft:air")]
        (when (not= bname "minecraft:air")
          (swap! non-air-count inc)
          (let [s-name (clojure.string/replace bname #"^minecraft:" "")]
            (swap! counts-by-block update s-name (fnil inc 0))))
        (let [idx (get-palette-idx b-dict)
              rel-pos [(- x min-x) (- y min-y) (- z min-z)]
              entry (cond-> {"pos" rel-pos "state" (int idx)}
                      (contains? world-entities [x y z])
                      (assoc "nbt" (get world-entities [x y z])))]
          (swap! blocks-list conj entry))))
    
    (let [sdf (SimpleDateFormat. "yyyyMMdd_HHmmss")
          timestamp (.format sdf (Date.))
          out-dir (cfg/blueprint-out-dir)
          meta-data
          {"name" "Base Principal"
           "timestamp" timestamp
           "bounds" {"min_x" min-x "max_x" max-x
                     "min_y" min-y "max_y" max-y
                     "min_z" min-z "max_z" max-z
                     "size_x" size-x "size_y" size-y "size_z" size-z}
           "stats" {"total_blocks" (count @blocks-list)
                    "non_air_blocks" @non-air-count
                    "containers_count" (count world-entities)
                    "top_materials" (into {} (take 15 (sort-by val > @counts-by-block)))}
           "palette" @palette
           "blocks" @blocks-list}
          
          json-file (io/file out-dir (str "base_principal_" timestamp ".json"))
          latest-json (io/file out-dir "base_principal_latest.json")]
      
      (spit json-file (json/write-str meta-data {:indent true}))
      (spit latest-json (json/write-str meta-data {:indent true}))
      
      ;; Mojang Structure NBT
      (let [palette-comp-list
            (mapv (fn [p]
                    (let [m {"Name" (get p "Name")}]
                      (if (seq (get p "Properties"))
                        (assoc m "Properties" (into {} (for [[pk pv] (get p "Properties")] [pk (str pv)])))
                        m)))
                  @palette)
            blocks-comp-list
            (mapv (fn [b]
                    {"pos" (mapv int (get b "pos"))
                     "state" (int (get b "state"))})
                  @blocks-list)
            root-nbt
            {"DataVersion" (int 3955)
             "size" [(int size-x) (int size-y) (int size-z)]
             "palette" palette-comp-list
             "blocks" blocks-comp-list
             "entities" []}
            
            world-d (cfg/world-dir)
            nbt-hist (io/file out-dir (str "base_principal_" timestamp ".nbt"))
            nbt-latest (io/file out-dir "base_principal_latest.nbt")
            nbt-world (io/file world-d "generated/minecraft/structures/base_principal.nbt")
            dp-mc (io/file world-d "datapacks/blueprints/data/minecraft/structure/base_principal.nbt")
            dp-bp (io/file world-d "datapacks/blueprints/data/blueprints/structure/base_principal.nbt")]
        
        (.mkdirs (.getParentFile nbt-world))
        (.mkdirs (.getParentFile dp-mc))
        (.mkdirs (.getParentFile dp-bp))
        
        (nbt/write-gzip-nbt! nbt-hist root-nbt)
        (nbt/write-gzip-nbt! nbt-latest root-nbt)
        (nbt/write-gzip-nbt! nbt-world root-nbt)
        (nbt/write-gzip-nbt! dp-mc root-nbt)
        (nbt/write-gzip-nbt! dp-bp root-nbt)
        
        [true (str "🏛️ Blueprint da Casa Principal salvo com sucesso! " @non-air-count " blocos preservados em NBT e JSON.") meta-data]))))

(defn restore-blueprint! []
  (let [bp-file (cfg/blueprint-path)]
    (if (.exists bp-file)
      (do
        (bridge/run-command! "place template blueprints:base_principal -164 99 -112")
        (bridge/run-command! "playsound minecraft:block.beacon.activate master @a -146 107 -85 1 1")
        [true "🏛️ Casa Principal restaurada com sucesso a partir do blueprint oficial!"])
      [false "Arquivo de blueprint 'base_principal.nbt' não encontrado."])))

(defn paste-structure!
  ([player-name] (paste-structure! player-name "base_principal"))
  ([player-name template-name]
   (bridge/run-command! "save-all")
   (if-let [nbt (inv/read-player-nbt player-name)]
     (let [pos (get nbt "Pos" [0.0 0.0 0.0])
           px (int (Math/round (double (first pos))))
           py (int (Math/round (double (second pos))))
           pz (int (Math/round (double (nth pos 2))))
           ox (- px 18)
           oy (- py 8)
           oz (- pz 27)]
       (builds/save-undo-state! :pasted-structure {:coords [ox oy oz (+ ox 37) (+ oy 19) (+ oz 47)]
                                                   :player player-name})
       (bridge/run-command! (str "place template blueprints:" (or template-name "base_principal") " " ox " " oy " " oz))
       (bridge/run-command! (str "playsound minecraft:block.beacon.activate master @a " px " " py " " pz " 1 1"))
       (bridge/run-command! (str "playsound minecraft:block.wood.place master @a " px " " py " " pz " 1 1"))
       [true (format "📋 Casa copiada e colada com sucesso em [%d, %d, %d] (Origem: [%d, %d, %d])!" px py pz ox oy oz)])
     [false (str "Jogador " player-name " não encontrado para determinar coordenadas.")])))

(defn undo-paste-structure! []
  (if-let [state (:pasted-structure (builds/get-undo-state))]
    (let [[min-x min-y min-z max-x max-y max-z] (:coords state)
          mid-y (quot (+ min-y max-y) 2)
          cmds [(str "fill " min-x " " (inc mid-y) " " min-z " " max-x " " max-y " " max-z " air replace")
                (str "fill " min-x " " min-y " " min-z " " max-x " " mid-y " " max-z " air replace")]]
      (doseq [c cmds] (bridge/run-command! c))
      (builds/save-undo-state! :pasted-structure nil)
      [true (format "↩️ Casa colada em [%d, %d, %d] desfeita e terreno restaurado com sucesso!" min-x min-y min-z)])
    [false "Nenhuma estrutura colada recentemente no histórico para desfazer."]))
