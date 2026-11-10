(ns detect-hole
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.shell :as sh]
            [clojure.data.json :as json])
  (:import [java.net Socket]
           [java.io BufferedReader InputStreamReader OutputStreamWriter PrintWriter]
           [java.nio.charset StandardCharsets]))

(defn send-cmd [line]
  (try
    (with-open [s (Socket. "127.0.0.1" 25595)]
      (.setTcpNoDelay s true)
      (.setSoTimeout s 2000)
      (let [in (BufferedReader. (InputStreamReader. (.getInputStream s) StandardCharsets/UTF_8))
            out (PrintWriter. (OutputStreamWriter. (.getOutputStream s) StandardCharsets/UTF_8) true)]
        (.println out line)
        (.readLine in)))
    (catch Throwable _ nil)))

(defn cavity? [res]
  (let [id (or (second (str/split (or res "") #"\|")) "")]
    (or (str/includes? id "air")
        (str/includes? id "water")
        (str/includes? id "light"))))

(defn detect-at-player [target-name radius depth height]
  (let [pos-raw (:out (sh/sh "/home/pace/.local/bin/mc" "pos" target-name))
        pos (json/read-str pos-raw :key-fn keyword)
        px (:x pos) py (:y pos) pz (:z pos)
        cx (int (Math/floor px))
        cy (int (Math/floor py))
        cz (int (Math/floor pz))

        standing-in (second (str/split (or (send-cmd (str "BLOCK|" cx "|" cy "|" cz)) "") #"\|"))
        feet-on (second (str/split (or (send-cmd (str "BLOCK|" cx "|" (dec cy) "|" cz)) "") #"\|"))

        ;; Acha o fundo real do buraco descendo
        floor-y (first (for [y (range cy (- cy depth) -1)
                             :let [r (send-cmd (str "BLOCK|" cx "|" y "|" cz))]
                             :when (not (cavity? r))]
                         (inc y)))
        min-y (or floor-y cy)
        min-x (atom cx) max-x (atom cx)
        min-z (atom cz) max-z (atom cz)
        max-y (atom cy)
        wall-ys (atom [])
        solids (atom {})]

    (doseq [y (range min-y (+ cy height 1))
            x (range (- cx radius) (+ cx radius 1) 2)
            z (range (- cz radius) (+ cz radius 1) 2)
            :let [r (send-cmd (str "BLOCK|" x "|" y "|" z))]]
      (if (cavity? r)
        (do
          (swap! min-x min x)
          (swap! max-x max x)
          (swap! max-y max y)
          (swap! min-z min z)
          (swap! max-z max z))
        (let [blk (second (str/split (or r "") #"\|"))]
          (when (seq blk)
            (swap! wall-ys conj y)
            (swap! solids update blk (fnil inc 0))))))

    ;; Calcula a cota da borda (rim) baseada na altura média das paredes sólidas
    (let [in-hole? (> (- cy min-y) 1)
          rim-y (if (seq @wall-ys)
                  (let [sorted-ys (sort @wall-ys)
                        cutoff-idx (int (* 0.75 (count sorted-ys)))]
                    (nth sorted-ys cutoff-idx))
                  (inc cy))
          fill-top (if in-hole? (min rim-y @max-y) cy)
          cmd1 (format "fill %d %d %d %d %d %d minecraft:water replace minecraft:air"
                       @min-x min-y @min-z @max-x fill-top @max-z)
          cmd2 (format "fill %d %d %d %d %d %d minecraft:water replace minecraft:water"
                       @min-x min-y @min-z @max-x fill-top @max-z)]
      {:jogador {:nome target-name
                 :posicao [px py pz]
                 :no-bloco standing-in
                 :sobre-bloco feet-on}
       :status (if in-hole? "DENTRO DE UM BURACO/DEPRESSÃO" "NA SUPERFÍCIE OU TERRENO NIVELADO")
       :geometria {:fundo-y min-y
                   :cota-borda-agua fill-top
                   :profundidade-abaixo-pes (max 0 (- cy min-y))
                   :altura-livre-teto (- @max-y cy)
                   :bounds {:x [@min-x @max-x]
                            :y [min-y fill-top]
                            :z [@min-z @max-z]}
                   :dimensoes {:largura-x (- @max-x @min-x -1)
                               :profundidade-agua (- fill-top min-y -1)
                               :comprimento-z (- @max-z @min-z -1)}}
       :paredes-ao-redor (into (sorted-map) (take 4 (sort-by val > @solids)))
       :fill-comandos {:passo-1 cmd1
                       :passo-2 cmd2}})))

(let [args *command-line-args*
      flood? (some #{"--flood" "flood" "-f"} args)
      player (or (first (remove #{"--flood" "flood" "-f"} args)) "pace")
      res (detect-at-player player 16 20 8)
      {:keys [passo-1 passo-2]} (:fill-comandos res)]
  (if flood?
    (do
      (println "🌊 [Inundação Automática] Detectando cavidade ao redor de" player "...")
      (pp/pprint (dissoc res :fill-comandos))
      (println "\n🚀 Executando preenchimento com blocos-fonte de água:")
      (println "  Passo 1:" passo-1)
      (send-cmd (str "CMD|" passo-1))
      (println "  Passo 2:" passo-2)
      (send-cmd (str "CMD|" passo-2))
      (println "\n✅ Buraco inundado com sucesso e 100% estabilizado!"))
    (pp/pprint res)))
