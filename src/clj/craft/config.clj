(ns craft.config
  "Unified configuration and environment-aware path resolvers."
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(defn get-env [k default-val]
  (let [v (System/getenv k)]
    (if (and v (not (empty? (.trim ^String v))))
      v
      default-val)))

(defn port []
  (Integer/parseInt (get-env "MC_PORT" "8090")))

(defn dry-run? []
  (= (get-env "MC_DRY_RUN" "0") "1"))

(defn fast-timers? []
  (= (get-env "MC_FAST_TIMERS" "0") "1"))

(defn config-dir ^File []
  (let [path (get-env "MC_CONFIG_DIR" (str (System/getProperty "user.home") "/.config/mc-cli"))
        f (io/file path)]
    (.mkdirs f)
    f))

(defn saves-dir ^File []
  (let [env-path (get-env "MC_SAVES_DIR" nil)]
    (cond
      (and env-path (.exists (io/file env-path))) (io/file env-path)
      (.exists (io/file "/home/pace/minecraft-server/world/playerdata")) (io/file "/home/pace/minecraft-server/world/playerdata")
      :else (io/file (str (System/getProperty "user.home") "/.minecraft/saves/(world)/playerdata")))))

(defn log-file ^File []
  (io/file (get-env "MC_LOG_FILE" "/home/pace/minecraft-server/logs/latest.log")))

(defn skins-dir ^File []
  (io/file (get-env "MC_SKINS_DIR" "/home/pace/minecraft-server/skins")))

(defn world-dir ^File []
  (io/file (get-env "MC_WORLD_DIR" "/home/pace/minecraft-server/world")))

(defn mca-path ^File []
  (io/file (get-env "MC_MCA_PATH" "/home/pace/minecraft-server/world/region/r.-1.-1.mca")))

(defn blueprint-path ^File []
  (io/file (get-env "MC_BLUEPRINT_PATH" "/home/pace/minecraft-server/world/generated/minecraft/structures/base_principal.nbt")))

(defn blueprint-out-dir ^File []
  (let [f (io/file (get-env "MC_BLUEPRINT_OUT_DIR" "/home/pace/minecraft-blueprints"))]
    (.mkdirs f)
    f))

(defn public-dir ^File []
  (io/file (get-env "MC_PUBLIC_DIR" "/home/pace/dev/craft-repl/resources/public")))

(def player-uuids
  {"pace"   "bfb04b46-41a1-3e85-b0fb-bc18eccdb3c6"
   "lizzie" "6d3fbe2d-c6f5-354f-b123-66dec0af223c"})

(def base-coords
  {:x -146 :y 107 :z -85})
