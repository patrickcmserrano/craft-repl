#!/usr/bin/env clojure
;;; Cliente nREPL minimo e scriptavel para o craft-repl (porta 7888).
;;;
;;; POR QUE ESTE ARQUIVO EXISTE
;;; O `make attach` injeta o agente e abre a 7888, e o `make repl` da um REPL
;;; INTERATIVO (nrepl.cmdline --connect) — mas nao havia cliente de UMA
;;; EXECUCAO, para usar de dentro de um script/pipe. Sem ele, medir dentro da
;;; JVM do servidor obrigava a escrever bencode a mao em Python (o `nc`,
;;; `telnet` e `socat` nao existem nesta maquina), e foi exactamente ali que a
;;; sessao de 2026-11-10 gastou as voltas: netstring com comprimento em BYTES
;;; vs caracteres, ordem das chaves, recv bloqueante, e o erro de compilacao
;;; que vem no campo `err` com status `eval-error`.
;;;
;;; Aqui NAO se embarca bencode nenhum: usa-se o `nrepl.bencode` que o proprio
;;; servidor usa — o mesmo parser dos dois lados do fio.
;;;
;;; REGRAS MEDIDAS (nREPL 1.3.0, Clojure 1.12, 2026-11-10):
;;; - **NAO envie `id`.** Esta versao TRAVA a resposta quando a mensagem tem
;;;   `id` (medido: `{"op":"clone","id":"c"}` -> timeout com zero bytes; o MESMO
;;;   payload sem `id` responde na hora). Casamos as respostas pela `session`.
;;; - clone ANTES do eval e obrigatorio; mensagem para sessao inexistente e
;;;   descartada em SILENCIO pelo nREPL (sem erro, sem journal).
;;; - o framing do nREPL e o proprio bencode (strings sao netstrings DENTRO
;;;   dele). NAO se envolve o payload num write-netstring: o servidor nao
;;;   decodifica e fecha a conexao (broken pipe).
;;; - o ns `user` da sessao NAO tem clojure.core referenciado: qualifique
;;;   (clojure.core/println) ou faca o require. Um `(require 'clojure.core)`
;;;   NAO traz as macros de volta; `(refer 'clojure.core)` traz.
;;; - o erro vem no campo `err` com status `eval-error`; um cliente que so olha
;;;   value/done sai antes de ler e devolve vazio.
;;;
;;; Uso:
;;;     clojure -M:scripts -m repl-client '(+ 1 2)'
;;;     clojure -M:scripts -m repl-client --port 7891 '(+ 1 2)'
;;;     clojure -M:scripts -m repl-client --file sonda.clj
;;;
;;; Sem o alias, use a forma direta (o `src/clj` e o `scripts` no path):
;;;     clojure -Sdeps '{:paths ["src/clj" "scripts"]}' -M -m repl-client '(+ 1 2)'
(ns repl-client
  (:require [clojure.string :as str]
            [nrepl.bencode :as bencode])
  (:import [java.net Socket InetSocketAddress]
           [java.io ByteArrayOutputStream OutputStream PushbackInputStream]))

(def ^:private ^:const default-host "127.0.0.1")
(def ^:private ^:const default-port 7888)

(defn- send-msg
  "Escreve uma mensagem bencode no stream.

  O framing do nREPL e o proprio bencode — NAO se envolve o payload num
  write-netstring. E NAO inclua :id: o nREPL 1.3.0 trava a resposta com ele."
  [^OutputStream out msg]
  (let [body (ByteArrayOutputStream.)]
    (bencode/write-bencode body msg)
    (.write out ^bytes (.toByteArray body))
    (.flush out)))

(defn- read-msg
  "Le uma mensagem bencode. Devolve nil no EOF."
  [^PushbackInputStream in]
  (try
    (bencode/read-bencode in)
    (catch java.io.EOFException _ nil)))

(defn- s [v]
  (cond (bytes? v) (String. ^bytes v "UTF-8")
        (nil? v) nil
        :else (str v)))

;; O nrepl.bencode devolve as MENSAGENS com chaves STRING (nao keyword) e os
;; valores como byte[]. Traduzir na entrada evita o bug classico: usar
;; (:session msg) num mapa de chaves string devolve sempre nil, e o read-until
;; nunca casa — o cliente fica pendurado esperando uma resposta que ja chegou.
(defn- norm-msg [msg]
  (when msg
    (into {} (map (fn [[k v]]
                    [(keyword (s k))
                     (if (vector? v) (mapv s v) (s v))])
                  msg))))

(defn- read-until
  "Le mensagens ate `done`/`eval-error` na resposta da sessao `where`."
  [^PushbackInputStream in where]
  (loop [out (StringBuilder.) err (StringBuilder.) value nil]
    (if-let [msg (norm-msg (read-msg in))]
      (if (not= where (:session msg))
        (recur out err value)
        (let [out (if-let [o (:out msg)] (.append out ^String o) out)
              err (if-let [e (:err msg)] (.append err ^String e) err)
              value (if (contains? msg :value) (:value msg) value)
              status (set (:status msg))]
          (if (or (contains? status "done") (contains? status "eval-error"))
            {:out (str out) :err (str err) :value value}
            (recur out err value))))
      {:out (str out) :err (str err) :value value})))

(defn- clone-session! [^PushbackInputStream in ^OutputStream out]
  (send-msg out {:op "clone"})
  (loop []
    (when-let [msg (norm-msg (read-msg in))]
      (if-let [sid (:new-session msg)]
        sid
        (recur)))))

(defn eval-code
  "Clona uma sessao e avalia `code` nela. `prelude` (opcional) e avaliado antes
  — util para um `require`, ja que o ns `user` nao tem clojure.core referido."
  ([host port code] (eval-code host port code nil))
  ([host port code prelude]
   (with-open [sock (Socket.)]
     (.connect sock (InetSocketAddress. ^String host (int port)) 10000)
     (let [in (PushbackInputStream. (.getInputStream sock))
           out (.getOutputStream sock)
           session (clone-session! in out)]
       (when prelude
         (send-msg out {:op "eval" :session session :code prelude})
         (read-until in session))
       (send-msg out {:op "eval" :session session :code code})
       (read-until in session)))))

(defn- parse-args [args]
  (loop [m {:host default-host :port default-port :code []}
         [a & more] args]
    (cond
      (nil? a) m
      (= a "--host") (recur (assoc m :host (first more)) (rest more))
      (= a "--port") (recur (assoc m :port (Long/parseLong (first more))) (rest more))
      (= a "--file") (recur (assoc m :file (first more)) (rest more))
      :else (recur (update m :code conj a) more))))

(defn -main [& args]
  (let [{:keys [host port file code]} (parse-args args)
        src (if file (slurp file) (str/join " " code))]
    (when (str/blank? src)
      (println "uso: clojure -M -m repl-client [--host H] [--port P] [--file f.clj] '<codigo>'")
      (System/exit 1))
    (let [{:keys [out err value]} (eval-code host port src)]
      (when-not (str/blank? err)
        (println "--- ERR ---")
        (println (str/trimr err)))
      (when-not (str/blank? out)
        (println (str/trimr out)))
      (when (some? value)
        (println (str "=> " value)))
      (System/exit 0))))
