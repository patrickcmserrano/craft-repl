(ns craft.world
  "Abstrações para blocos e chunks no mundo do Minecraft."
  (:require [craft.core :as core]))

(defn get-overworld
  "Retorna a instância de ServerWorld correspondente ao Overworld."
  []
  (when-let [server (core/find-minecraft-server)]
    (.getOverworld server)))

(defn get-block-at
  "Lê o estado do bloco na coordenada [x y z] dada."
  [[x y z]]
  (when-let [world (get-overworld)]
    (let [pos-class (Class/forName "net.minecraft.util.math.BlockPos" true (.getClassLoader (class world)))
          pos-ctor  (.getConstructor pos-class (into-array Class [Integer/TYPE Integer/TYPE Integer/TYPE]))
          pos       (.newInstance pos-ctor (into-array Object [(int x) (int y) (int z)]))
          state     (.getBlockState world pos)]
      {:pos   [x y z]
       :state (str state)})))
