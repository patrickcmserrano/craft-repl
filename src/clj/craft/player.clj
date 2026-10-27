(ns craft.player
  "Abstrações para consulta e manipulação de jogadores em formato de dados puros."
  (:require [craft.core :as core]
            [clojure.string :as str])
  (:import [java.lang.reflect Method]))

(defn- player-manager
  []
  (when-let [server (core/find-minecraft-server)]
    (or (try (.getPlayerManager server) (catch Throwable _ nil))
        (try (let [m (some #(when (re-find #"class_3324|PlayerManager" (str (.getReturnType %))) %)
                           (.getMethods (.getClass server)))]
               (.invoke m server (into-array Object [])))
             (catch Throwable _ nil)))))

(defn- all-players-raw
  []
  (when-let [pm (player-manager)]
    (or (try (.getPlayerList pm) (catch Throwable _ nil))
        (try (let [m (some #(when (and (= 0 (count (.getParameterTypes %)))
                                       (re-find #"List" (str (.getReturnType %)))) %)
                           (.getMethods (.getClass pm)))]
               (.invoke m pm (into-array Object [])))
             (catch Throwable _ nil)))))

(defn get-player
  "Retorna a instância do ServerPlayerEntity correspondente ao nome."
  [player-name]
  (some (fn [p]
          (let [name (try (.. p getGameProfile getName)
                          (catch Throwable _
                            (try (.getName (.invoke (.getMethod (.getClass p) "method_7334" (into-array Class [])) p (into-array Object [])))
                                 (catch Throwable _ nil))))]
            (when (= (str/lower-case (or name "")) (str/lower-case (str player-name)))
              p)))
        (all-players-raw)))

(defn player-coords
  "Retorna as coordenadas do jogador como um mapa EDN."
  [player-name]
  (when-let [p (get-player player-name)]
    (let [x (try (.getX p) (catch Throwable _ (.. p method_23317)))
          y (try (.getY p) (catch Throwable _ (.. p method_23318)))
          z (try (.getZ p) (catch Throwable _ (.. p method_23321)))]
      {:name player-name
       :x    (double x)
       :y    (double y)
       :z    (double z)})))

(defn players
  "Retorna lista de todos os jogadores conectados e suas coordenadas."
  []
  (mapv (fn [p]
          (let [name (try (.. p getGameProfile getName)
                          (catch Throwable _
                            (try (.getName (.invoke (.getMethod (.getClass p) "method_7334" (into-array Class [])) p (into-array Object [])))
                                 (catch Throwable _ "unknown"))))
                x (try (.getX p) (catch Throwable _ (.. p method_23317)))
                y (try (.getY p) (catch Throwable _ (.. p method_23318)))
                z (try (.getZ p) (catch Throwable _ (.. p method_23321)))]
            {:name name
             :x    (double x)
             :y    (double y)
             :z    (double z)}))
        (all-players-raw)))

(defn teleport!
  "Teleporta um jogador para as coordenadas especificadas."
  [player-name x y z]
  (core/run-command! (format "tp %s %.1f %.1f %.1f" (str player-name) (double x) (double y) (double z))))

(defn heal!
  "Cura e restaura comida do jogador."
  [player-name]
  (core/run-command! (format "effect give %s instant_health 1 255" (str player-name)))
  (core/run-command! (format "effect give %s saturation 1 255" (str player-name))))

(defn give!
  "Entrega um item ao jogador."
  [player-name item count]
  (core/run-command! (format "give %s %s %d" (str player-name) (str item) (int count))))
