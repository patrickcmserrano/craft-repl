(ns craft.fixtures
  "Fixtures and binary synthetic generator for isolated, safe testing of Minecraft tools."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream File FileOutputStream]
           [java.nio ByteBuffer ByteOrder]
           [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]
           [java.util.zip GZIPOutputStream Deflater DeflaterOutputStream]))

(defn write-nbt-tag! [^ByteBuffer buf tag-type ^String name write-val-fn]
  (.put buf (byte tag-type))
  (let [name-bytes (.getBytes name "UTF-8")]
    (.putShort buf (short (alength name-bytes)))
    (.put buf name-bytes))
  (write-val-fn buf))

(defn write-nbt-string! [^ByteBuffer buf ^String s]
  (let [b (.getBytes s "UTF-8")]
    (.putShort buf (short (alength b)))
    (.put buf b)))

(defn write-player-dat!
  "Writes a synthetic GZIP-compressed player .dat file with inventory and telemetry."
  [^File file {:keys [x y z yaw pitch health food xp inventory]
               :or {x -146.0 y 107.0 z -85.0 yaw 0.0 pitch 0.0 health 20.0 food 20 xp 0 inventory []}}]
  (let [baos (ByteArrayOutputStream.)
        gzos (GZIPOutputStream. baos)
        buf (ByteBuffer/allocate (* 64 1024))]
    (.order buf ByteOrder/BIG_ENDIAN)
    ;; Root compound
    (.put buf (byte 10)) ;; TAG_Compound
    (write-nbt-string! buf "")
    
    ;; Pos (List of 3 Doubles)
    (write-nbt-tag! buf 9 "Pos"
                    (fn [b]
                      (.put b (byte 6)) ;; TAG_Double
                      (.putInt b 3)
                      (.putDouble b (double x))
                      (.putDouble b (double y))
                      (.putDouble b (double z))))

    ;; Rotation (List of 2 Floats)
    (write-nbt-tag! buf 9 "Rotation"
                    (fn [b]
                      (.put b (byte 5)) ;; TAG_Float
                      (.putInt b 2)
                      (.putFloat b (float yaw))
                      (.putFloat b (float pitch))))

    ;; Health, foodLevel, XpLevel
    (write-nbt-tag! buf 5 "Health" (fn [b] (.putFloat b (float health))))
    (write-nbt-tag! buf 3 "foodLevel" (fn [b] (.putInt b (int food))))
    (write-nbt-tag! buf 3 "XpLevel" (fn [b] (.putInt b (int xp))))

    ;; Inventory (List of Compounds)
    (write-nbt-tag! buf 9 "Inventory"
                    (fn [b]
                      (.put b (byte 10)) ;; TAG_Compound
                      (.putInt b (count inventory))
                      (doseq [item inventory]
                        ;; id
                        (write-nbt-tag! b 8 "id" (fn [ib] (write-nbt-string! ib (:id item))))
                        ;; count
                        (write-nbt-tag! b 2 "count" (fn [ib] (.putShort ib (short (:count item 1)))))
                        ;; Slot
                        (write-nbt-tag! b 1 "Slot" (fn [ib] (.put ib (byte (:slot item 0)))))
                        ;; components
                        (when (or (:damage item) (:enchantments item) (:custom_name item))
                          (write-nbt-tag! b 10 "components"
                                          (fn [cb]
                                            (when-let [dmg (:damage item)]
                                              (write-nbt-tag! cb 3 "minecraft:damage" (fn [db] (.putInt db (int dmg)))))
                                            (when-let [cname (:custom_name item)]
                                              (write-nbt-tag! cb 8 "minecraft:custom_name" (fn [db] (write-nbt-string! db cname))))
                                            (when-let [ench (:enchantments item)]
                                              (write-nbt-tag! cb 10 "minecraft:enchantments"
                                                              (fn [eb]
                                                                (write-nbt-tag! eb 10 "levels"
                                                                                (fn [lb]
                                                                                  (doseq [[k v] ench]
                                                                                    (write-nbt-tag! lb 3 (name k) (fn [vb] (.putInt vb (int v)))))
                                                                                  (.put lb (byte 0))))
                                                                (.put eb (byte 0)))))
                                            (.put cb (byte 0)))))
                        (.put b (byte 0)))))
    (.put buf (byte 0)) ;; End of root compound
    (.flip buf)
    (let [arr (byte-array (.remaining buf))]
      (.get buf arr)
      (.write gzos arr)
      (.finish gzos)
      (.close gzos))
    (with-open [fos (FileOutputStream. file)]
      (.write fos (.toByteArray baos)))))

(defn write-synthetic-mca!
  "Creates a minimal valid Anvil .mca region file for chunk r.-1.-1.mca with palette."
  [^File file]
  (let [buf (ByteBuffer/allocate (* 1024 1024))]
    (.order buf ByteOrder/BIG_ENDIAN)
    ;; 1024 chunk table entries (4 bytes each)
    ;; We write chunk at rx=22, rz=27 (corresponding to -10, -5 chunk coords => cx -160, cz -80)
    (let [rx 22 rz 27
          idx (* 4 (+ (mod rx 32) (* 32 (mod rz 32))))
          sector 2]
      (.position buf idx)
      ;; 3 bytes sector offset + 1 byte sector count
      (.put buf (byte (bit-shift-right sector 16)))
      (.put buf (byte (bit-shift-right sector 8)))
      (.put buf (byte sector))
      (.put buf (byte 1))) ;; 1 sector (4096 bytes)

    ;; Timestamps table at 4096 (4096 to 8191)
    (.position buf 4096)
    (dotimes [_ 1024] (.putInt buf (int 0)))

    ;; Sector 2 at 8192: Chunk payload
    (.position buf 8192)
    ;; Write compressed NBT chunk with block_states & block_entities
    (let [cbuf (ByteBuffer/allocate (* 64 1024))]
      (.order cbuf ByteOrder/BIG_ENDIAN)
      (.put cbuf (byte 10)) ;; root compound
      (write-nbt-string! cbuf "")
      (write-nbt-tag! cbuf 3 "xPos" (fn [b] (.putInt b -10)))
      (write-nbt-tag! cbuf 3 "zPos" (fn [b] (.putInt b -5)))
      (write-nbt-tag! cbuf 3 "DataVersion" (fn [b] (.putInt b 3955)))
      
      ;; sections list
      (write-nbt-tag! cbuf 9 "sections"
                      (fn [b]
                        (.put b (byte 10)) ;; TAG_Compound
                        (.putInt b 1) ;; 1 section
                        (write-nbt-tag! b 1 "Y" (fn [sb] (.put sb (byte 6)))) ;; Y=6 => 96..111
                        (write-nbt-tag! b 10 "block_states"
                                        (fn [sb]
                                          (write-nbt-tag! sb 9 "palette"
                                                          (fn [pb]
                                                            (.put pb (byte 10))
                                                            (.putInt pb 1)
                                                            (write-nbt-tag! pb 8 "Name" (fn [nb] (write-nbt-string! nb "minecraft:stone")))
                                                            (.put pb (byte 0))))
                                          (.put sb (byte 0))))
                        (.put b (byte 0))))
      
      ;; block_entities list
      (write-nbt-tag! cbuf 9 "block_entities"
                      (fn [b]
                        (.put b (byte 10))
                        (.putInt b 0)))
      (.put cbuf (byte 0)) ;; root end
      (.flip cbuf)
      (let [raw (byte-array (.remaining cbuf))
            _ (.get cbuf raw)
            cbaos (ByteArrayOutputStream.)
            _ (with-open [dos (DeflaterOutputStream. cbaos (Deflater. Deflater/DEFAULT_COMPRESSION))]
                (.write dos raw))
            comp-bytes (.toByteArray cbaos)
            length (inc (alength comp-bytes))]
        (.putInt buf length)
        (.put buf (byte 2)) ;; zlib compression
        (.put buf comp-bytes)))

    (with-open [fos (FileOutputStream. file)]
      (.write fos (.array buf) 0 (max 12288 (.position buf))))))

(defn create-sandbox!
  "Creates a fully isolated sandbox directory tree for testing."
  []
  (let [root (.toFile (Files/createTempDirectory "craft-test-sandbox-" (into-array FileAttribute [])))
        saves-dir (io/file root "world" "playerdata")
        region-dir (io/file root "world" "region")
        logs-dir (io/file root "logs")
        config-dir (io/file root "config" "mc-cli")
        skins-dir (io/file root "skins")
        blueprints-dir (io/file root "blueprints")
        world-dir (io/file root "world")]
    (.mkdirs saves-dir)
    (.mkdirs region-dir)
    (.mkdirs logs-dir)
    (.mkdirs config-dir)
    (.mkdirs skins-dir)
    (.mkdirs blueprints-dir)
    
    ;; Write default pace.dat & lizzie.dat
    (write-player-dat! (io/file saves-dir "bfb04b46-41a1-3e85-b0fb-bc18eccdb3c6.dat")
                       {:x -146.0 :y 107.0 :z -85.0 :yaw 180.0 :health 20.0
                        :inventory [{:id "minecraft:diamond_sword" :count 1 :slot 0 :damage 15 :custom_name "Lamina Ancestral"}
                                    {:id "minecraft:iron_pickaxe" :count 1 :slot 1 :damage 0}
                                    {:id "minecraft:iron_ingot" :count 32 :slot 9}
                                    {:id "minecraft:stone" :count 64 :slot 10}
                                    {:id "minecraft:elytra" :count 1 :slot 102 :damage 25}
                                    {:id "minecraft:diamond_chestplate" :count 1 :slot 11 :damage 50
                                     :enchantments {:protection 4 :unbreaking 3}}]})

    (write-player-dat! (io/file saves-dir "6d3fbe2d-c6f5-354f-b123-66dec0af223c.dat")
                       {:x -150.0 :y 107.0 :z -80.0 :yaw 90.0 :health 18.5
                        :inventory [{:id "minecraft:bread" :count 16 :slot 0}]})

    ;; Write skin dummy files
    (spit (io/file skins-dir "pace.png") "dummy-png-bytes")
    (spit (io/file skins-dir "pace_current.png") "dummy-png-bytes")
    (spit (io/file skins-dir "lizzie.png") "dummy-png-bytes")

    ;; Write synthetic r.-1.-1.mca
    (write-synthetic-mca! (io/file region-dir "r.-1.-1.mca"))

    ;; Write waypoints.json & vault.json
    (spit (io/file config-dir "waypoints.json")
          (json/write-str [{:name "Base Principal" :x -146 :y 107 :z -85 :icon "🏠"}]))
    (spit (io/file config-dir "vault.json") "[]")

    ;; Write mock latest.log for parse_chest_slots test
    (spit (io/file logs-dir "latest.log")
          "-160, 107, -89 has the following block data: [{Slot: 0b, count: 16, id: \"minecraft:iron_ingot\"}]\n")

    {:root root
     :saves-dir saves-dir
     :region-dir region-dir
     :logs-dir logs-dir
     :config-dir config-dir
     :skins-dir skins-dir
     :blueprints-dir blueprints-dir
     :world-dir world-dir}))

(defn read-recorded-commands!
  "Reads all commands recorded by MC_DRY_RUN in sandbox config."
  [sandbox]
  (let [log-file (io/file (:config-dir sandbox) "dry_run_commands.jsonl")]
    (if (.exists log-file)
      (mapv #(json/read-str % :key-fn keyword) (line-seq (io/reader log-file)))
      [])))
