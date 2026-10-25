#!/usr/bin/env bb
(ns attach
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))

(defn find-minecraft-pid []
  (let [res (shell {:out :string} "jps -v")
        lines (str/split-lines (:out res))]
    (some (fn [line]
            (when (or (str/includes? line "net.fabricmc")
                      (str/includes? line "Minecraft")
                      (str/includes? line "fabric"))
              (first (str/split (str/trim line) #"\s+"))))
          lines)))

(let [pid (or (first *command-line-args*)
              (find-minecraft-pid))]
  (if-not pid
    (do
      (println "[-] Não foi possível detectar o PID do Minecraft automaticamente.")
      (println "Uso: bb scripts/attach.clj [PID] [PORTA]")
      (System/exit 1))
    (let [port (or (second *command-line-args*) "7888")]
      (println (format "[+] Minecraft detectado com PID: %s" pid))
      (println (format "[*] Disparando injeção do craft-repl na porta %s..." port))
      (shell "make" "attach" (str "PID=" pid) (str "PORT=" port)))))
