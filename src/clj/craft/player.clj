(ns craft.player
  "Abstrações para consulta e manipulação de jogadores em formato de dados puros."
  (:require [craft.core :as core]))

(defn get-player
  "Retorna a instância do ServerPlayerEntity correspondente ao nome."
  [name]
  (when-let [server (core/find-minecraft-server)]
    (let [player-manager (.. server getPlayerManager)]
      (.getPlayer player-manager name))))

(defn player-coords
  "Retorna as coordenadas e dimensão do jogador como um mapa EDN."
  [name]
  (when-let [player (get-player name)]
    {:name      name
     :x         (.getX player)
     :y         (.getY player)
     :z         (.getZ player)
     :yaw       (.getYaw player)
     :pitch     (.getPitch player)
     :dimension (str (.. player getWorld getRegistryKey getValue))}))

(defn teleport!
  "Teleporta um jogador para as coordenadas especificadas dentro do ServerThread."
  [name x y z]
  (core/in-server-thread
    (when-let [player (get-player name)]
      (let [world (.getWorld player)]
        (.teleport player world (double x) (double y) (double z) (.getYaw player) (.getPitch player))
        (println (format "[craft.player] Jogador %s teleportado para [%.1f, %.1f, %.1f]"
                         name (double x) (double y) (double z)))))))

(defn add-xp!
  "Adiciona pontos de experiência ao jogador dentro do ServerThread."
  [name amount]
  (core/in-server-thread
    (when-let [player (get-player name)]
      (.addExperience player (int amount))
      (println (format "[craft.player] Creditados %d pontos de XP para %s" (int amount) name)))))
