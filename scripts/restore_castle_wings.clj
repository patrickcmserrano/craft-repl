(ns restore-castle-wings
  (:require [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [java.net Socket]
           [java.io BufferedReader InputStreamReader OutputStreamWriter PrintWriter]
           [java.nio.charset StandardCharsets]))

(defonce conn-atom (atom nil))

(defn get-conn []
  (let [c @conn-atom]
    (if (and c (not (.isClosed ^Socket (:sock c))))
      c
      (let [s (Socket. "127.0.0.1" 25595)]
        (.setTcpNoDelay s true)
        (.setSoTimeout s 3000)
        (let [in (BufferedReader. (InputStreamReader. (.getInputStream s) StandardCharsets/UTF_8))
              out (PrintWriter. (OutputStreamWriter. (.getOutputStream s) StandardCharsets/UTF_8) true)
              new-c {:sock s :in in :out out}]
          (reset! conn-atom new-c)
          new-c)))))

(defn send-cmd! [line]
  (try
    (let [{:keys [^BufferedReader in ^PrintWriter out]} (get-conn)]
      (.println out line)
      (.readLine in))
    (catch Throwable _
      (reset! conn-atom nil)
      nil)))

(defn get-block [x y z]
  (second (str/split (or (send-cmd! (str "BLOCK|" x "|" y "|" z)) "") #"\|")))

(defn air? [b]
  (or (nil? b) (str/includes? b "air") (str/includes? b "light")))

;; ZONA DE EXCLUSÃO BLINDADA (Torre de obras do Pace e Lizzie)
(defn in-exclusion-zone? [x y z]
  (and (<= -60 x -30)
       (>= z 220)
       (>= y 115)))

(defn safe-setblock! [x y z block-state]
  (if (in-exclusion-zone? x y z)
    (println (format "⛔ BLOQUEADO: Tentativa de alterar bloco na zona protegida [%d %d %d]" x y z))
    (send-cmd! (str "CMD|setblock " x " " y " " z " " block-state " replace"))))

(defn run-restoration! []
  (println "🏰 Iniciando restauração e blindagem ultrarrápida do Castelo dos Touros Negros...")
  (println "🛡️ ZONA PROTEGIDA: X:[-60..-30], Z:[220..255], Y:[115..160] (Torre de obras 100% intocada).")

  (let [floors-fixed (atom 0)
        roofs-sealed (atom 0)
        lights-placed (atom 0)]

    ;; 1. VEDAR OS PISOS DE QUEDA LIVRE NAS ALTURAS CHAVE (Y=88..116)
    (println "\n[*] 1. Blindando pisos com buracos de queda livre nas alas Norte e Central...")
    (doseq [y [88 90 92 94 96 98 100 102 104 106 108 110 112 114]
            x (range -65 5 2)
            z (range 155 218 2)
            :when (not (in-exclusion-zone? x y z))]
      (let [b (get-block x y z)]
        (when (air? b)
          ;; Verifica se há piso sólido ao lado
          (let [adj-solid (count (filter #(not (air? (get-block (+ x (first %)) y (+ z (second %)))))
                                         [[-1 0] [1 0] [0 -1] [0 1]]))
                b-down (get-block x (dec y) z)
                b-deep (get-block x (- y 4) z)]
            (when (and (>= adj-solid 2) (air? b-down) (air? b-deep))
              (safe-setblock! x y z "minecraft:dark_oak_planks")
              (swap! floors-fixed inc))))))

    (println (format "  ✅ %d vãos de piso foram blindados com dark oak!" @floors-fixed))

    ;; 2. FECHAR FRESTAS E BURACOS NOS TELHADOS (Y=115..130)
    (println "\n[*] 2. Fechando furos de telhado e cúpulas abertas...")
    (doseq [y (range 115 130 2)
            x (range -65 0 2)
            z (range 160 216 2)
            :when (not (in-exclusion-zone? x y z))]
      (let [b (get-block x y z)]
        (when (air? b)
          (let [adj-roof (count (filter #(not (air? (get-block (+ x (first %)) y (+ z (second %)))))
                                        [[-1 0] [1 0] [0 -1] [0 1]]))
                below (get-block x (dec y) z)]
            (when (and (>= adj-roof 3) (air? below))
              (safe-setblock! x y z "minecraft:polished_blackstone_slab[type=bottom]")
              (swap! roofs-sealed inc))))))

    (println (format "  ✅ %d frestas de telhado foram vedadas com blackstone!" @roofs-sealed))

    ;; 3. ILUMINAÇÃO OCULTA ANTI-MOB
    (println "\n[*] 3. Adicionando iluminação oculta (anti-monstros) nas alas altas...")
    (doseq [y (range 92 125 6)
            x (range -65 0 8)
            z (range 160 215 8)
            :when (not (in-exclusion-zone? x y z))]
      (let [b (get-block x y z)
            b-below (get-block x (dec y) z)]
        (when (and (air? b) (not (air? b-below)))
          (safe-setblock! x y z "minecraft:light[level=14]")
          (swap! lights-placed inc))))

    (println (format "  ✅ %d pontos de iluminação oculta aplicados!" @lights-placed))

    {:pisos-fechados @floors-fixed
     :telhados-vedados @roofs-sealed
     :luzes-instaladas @lights-placed
     :status "RESTAURAÇÃO CONCLUÍDA"}))

(let [res (run-restoration!)]
  (pp/pprint res))
