(ns craft.bridge
  "Unified Minecraft command runner supporting dry-run logging, shortcuts, chaining, and nREPL/CLI execution."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [craft.config :as cfg]
            [craft.core :as core])
  (:import [java.io File]
           [java.util.concurrent TimeUnit]))

(def ^:private mc-shortcuts
  #{"home" "sethome" "pull" "goto" "golem" "day" "night" "sun"
    "vision" "skin-backup" "skin-restore" "skin" "heal" "keepinv"
    "sculk-base" "undo-sculk" "backup" "restore"})

(defn- log-dry-run! [cmd]
  (let [dry-file (io/file (cfg/config-dir) "dry_run_commands.jsonl")
        line (json/write-str {:time (double (/ (System/currentTimeMillis) 1000.0))
                              :cmd cmd})]
    (spit dry-file (str line "\n") :append true)))

(defn- run-cli-single [^String cmd]
  (let [words (str/split cmd #"\s+" 2)
        first-word (first words)
        mc-bin (str (System/getProperty "user.home") "/.local/bin/mc")
        args (if (contains? mc-shortcuts first-word)
               (into [mc-bin] (str/split cmd #"\s+"))
               [mc-bin cmd])]
    (try
      (let [pb (ProcessBuilder. ^java.util.List args)
            proc (.start pb)
            finished? (.waitFor proc 8 TimeUnit/SECONDS)]
        (if finished?
          (let [stdout (slurp (.getInputStream proc))
                stderr (slurp (.getErrorStream proc))
                out (str/trim (if-not (str/blank? stdout) stdout stderr))
                code (.exitValue proc)]
            [(= 0 code) out])
          (do
            (.destroyForcibly proc)
            [false "Comando expirou tempo limite"])))
      (catch Exception e
        [false (.getMessage e)]))))

(defn run-command!
  "Executes one or more Minecraft commands (chained with && or ;)."
  [cmd-str]
  (let [raw (or cmd-str "")
        sub-cmds (->> (str/split (str/replace raw #";" "&&") #"&&")
                      (map str/trim)
                      (remove str/blank?))]
    (if (empty? sub-cmds)
      [true ""]
      (let [dry? (cfg/dry-run?)]
        (loop [remaining sub-cmds
               acc-out []
               all-ok? true]
          (if (empty? remaining)
            [all-ok? (if (seq acc-out) (str/join " | " acc-out) "⚡ OK")]
            (let [sub (first remaining)
                  ;; Clean prefixes
                  sub (cond
                        (str/starts-with? sub "/") (subs sub 1)
                        (str/starts-with? sub "/home/pace/.local/bin/mc ") (subs sub (count "/home/pace/.local/bin/mc "))
                        (str/starts-with? sub "mc ") (subs sub 3)
                        :else sub)
                  sub (str/trim sub)
                  ;; Replace @s with pace
                  sub (str/replace sub #"@s\b" "pace")]
              (if dry?
                (do
                  (log-dry-run! sub)
                  (recur (rest remaining)
                         (conj acc-out (str "⚡ [DRY-RUN] /" sub))
                         all-ok?))
                (let [[ok? out] (if @core/cached-server
                                  (try
                                    (core/run-command! sub)
                                    [true ""]
                                    (catch Exception _
                                      (run-cli-single sub)))
                                  (run-cli-single sub))]
                  (recur (rest remaining)
                         (if-not (str/blank? out) (conj acc-out out) acc-out)
                         (and all-ok? ok?)))))))))))
