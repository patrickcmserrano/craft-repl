(ns craft.dashboard.server
  "Ring & http-kit HTTP server delivering the unified Clojure dashboard backend on port 8090."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [craft.blueprint :as blueprint]
            [craft.bridge :as bridge]
            [craft.builds :as builds]
            [craft.chests :as chests]
            [craft.config :as cfg]
            [craft.dashboard.proxy :as proxy]
            [craft.inventory :as inv]
            [org.httpkit.server :as http-server])
  (:import [java.io ByteArrayOutputStream DataInputStream DataOutputStream File InputStream RandomAccessFile]
           [java.net InetSocketAddress Socket]))

(defn json-resp
  ([body] (json-resp 200 body))
  ([status body]
   {:status status
    :headers {"Content-Type" "application/json; charset=utf-8"
              "Access-Control-Allow-Origin" "*"}
    :body (json/write-str body)}))

(defn is-server-running? []
  (try
    (with-open [s (Socket.)]
      (.connect s (InetSocketAddress. "127.0.0.1" 25565) 250)
      true)
    (catch Exception _ false)))

(defn- read-varint [^InputStream in]
  (loop [result (int 0), shift (int 0)]
    (let [b (.read in)]
      (if (neg? b)
        result
        (let [value (bit-and b 0x7f)
              new-res (bit-or result (bit-shift-left value shift))]
          (if (zero? (bit-and b 0x80))
            new-res
            (recur new-res (int (+ shift 7)))))))))

(defn query-slp-players
  "Queries Minecraft Server List Ping (SLP) on localhost:25565.
   Returns {:online-count int, :player-names #{...}} or nil on failure."
  ([] (query-slp-players "127.0.0.1" 25565 300))
  ([host port timeout-ms]
   (try
     (with-open [s (Socket.)]
       (.connect s (InetSocketAddress. ^String host (int port)) (int timeout-ms))
       (.setSoTimeout s (int timeout-ms))
       (let [out (.getOutputStream s)
             in (.getInputStream s)
             baos (ByteArrayOutputStream.)
             dos (DataOutputStream. baos)]
         ;; Handshake packet: id 0x00, protocol version 767, host, port, next state 1 (status)
         (.writeByte dos 0x00)
         (.writeByte dos 0xff)
         (.writeByte dos 0x05)
         (let [hb (.getBytes host "UTF-8")]
           (.writeByte dos (count hb))
           (.write dos hb))
         (.writeShort dos (int port))
         (.writeByte dos 0x01)
         (let [hb (.toByteArray baos)]
           (.write out (count hb))
           (.write out hb))
         ;; Status request packet: length 1, id 0
         (.write out (byte-array [1 0]))
         (.flush out)
         ;; Read response packet
         (let [_pkt-len (read-varint in)
               _pkt-id (read-varint in)
               str-len (read-varint in)
               data (byte-array str-len)
               _ (.readFully (DataInputStream. in) data)
               raw-json (String. data "UTF-8")
               parsed (json/read-str raw-json :key-fn keyword)
               p-info (:players parsed)
               count (:online p-info 0)
               sample (:sample p-info [])
               names (into #{} (keep #(when-let [n (:name %)] (str/lower-case (str n)))) sample)]
           {:online-count (int count)
            :player-names names})))
     (catch Exception _ nil))))

(defn online-players-from-log
  "Scans the tail of latest.log to determine currently online players."
  ([] (online-players-from-log (cfg/log-file)))
  ([log-file]
   (let [^File f (io/file log-file)]
     (if-not (.exists f)
       #{}
       (try
         (with-open [r (RandomAccessFile. f "r")]
           (let [len (.length r)
                 seek-pos (max 0 (- len 65536))]
             (.seek r seek-pos)
             (let [lines (loop [acc []]
                           (if-let [line (.readLine r)]
                             (recur (conj acc line))
                             acc))]
               (reduce (fn [online line]
                         (cond
                           (re-find #"Stopping server" line)
                           #{}
                           (re-find #"(\w+) joined the game" line)
                           (let [[_ name] (re-find #"(\w+) joined the game" line)]
                             (conj online (str/lower-case name)))
                           (re-find #"(\w+) left the game" line)
                           (let [[_ name] (re-find #"(\w+) left the game" line)]
                             (disj online (str/lower-case name)))
                           :else online))
                       #{}
                       lines))))
         (catch Exception _ #{}))))))

(defn get-online-players
  "Returns a set of lower-case player names currently online on the server."
  []
  (if-not (is-server-running?)
    #{}
    (if-let [slp (query-slp-players)]
      (if (zero? (:online-count slp))
        #{}
        (if (seq (:player-names slp))
          (:player-names slp)
          (online-players-from-log)))
      (online-players-from-log))))

(defn get-telemetry []
  (let [online? (is-server-running?)
        online-players (if online? (get-online-players) #{})
        players-data
        (vec
         (for [p-name ["pace" "lizzie"]
               :let [nbt (inv/read-player-nbt p-name)]
               :when nbt]
           (let [pos (get nbt "Pos" [0.0 0.0 0.0])
                 rot (get nbt "Rotation" [0.0 0.0])
                 health (double (get nbt "Health" 20.0))
                 p-online? (contains? online-players p-name)]
             {:name (if (= p-name "pace") "Pace" "Lizzie")
              :online (boolean p-online?)
              :x (double (first pos))
              :y (double (second pos))
              :z (double (nth pos 2))
              :yaw (double (first rot))
              :pitch (double (second rot))
              :health (double health)})))
        wp-file (io/file (cfg/config-dir) "waypoints.json")
        waypoints (if (.exists wp-file)
                    (try (json/read-str (slurp wp-file) :key-fn keyword) (catch Exception _ []))
                    [])]
    {:server_online online?
     :players players-data
     :waypoints (mapv (fn [w] {:name (str (:name w))
                               :x (int (:x w 0))
                               :y (int (:y w 0))
                               :z (int (:z w 0))
                               :icon (str (:icon w "📍"))})
                      waypoints)}))

(defn get-inventory-response [player-name]
  (let [raw (inv/get-player-inventory player-name)]
    (if (:error raw)
      {:error (:error raw)}
      (let [all-slots (:main raw)
            hotbar (subvec all-slots 0 9)
            main (subvec all-slots 9 36)
            armor {:helmet (:head (:armor raw))
                   :chestplate (:chest (:armor raw))
                   :leggings (:legs (:armor raw))
                   :boots (:feet (:armor raw))}
            offhand (:offhand raw)
            total-items (count (filter some? (concat all-slots (vals armor) (when offhand [offhand]))))]
        {:player (or player-name "pace")
         :health (:health raw 20.0)
         :food (:food raw 20)
         :xp (:xp raw 0)
         :hotbar hotbar
         :main main
         :armor armor
         :offhand offhand
         :total_items (int total-items)}))))

(defn parse-json-body [req]
  (when-let [body (:body req)]
    (let [s (if (string? body) body (slurp body))]
      (when-not (str/blank? s)
        (try (json/read-str s :key-fn keyword) (catch Exception _ {}))))))

(defn parse-query-params [query-str]
  (if (str/blank? query-str)
    {}
    (into {}
          (for [part (str/split query-str #"&")
                :let [[k v] (str/split part #"=" 2)]]
            [(str/lower-case (or k "")) (java.net.URLDecoder/decode (or v "") "UTF-8")]))))

(defn serve-static-file [^File file mime-type]
  (if (.exists file)
    {:status 200
     :headers {"Content-Type" mime-type
               "Cache-Control" "no-cache"}
     :body (slurp file)}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "404 Not Found"}))

(defn get-param [params body k default-val]
  (let [k-str (name k)
        k-kw (keyword k)]
    (or (get body k-kw)
        (get body k-str)
        (get params k-str)
        (get params (str/lower-case k-str))
        default-val)))

(defn handler [req]
  (let [uri (:uri req)
        method (:request-method req)
        params (parse-query-params (:query-string req))]
    (cond
      ;; 1. Root & Static Frontend
      (= uri "/")
      (serve-static-file (io/file (cfg/public-dir) "index.html") "text/html; charset=utf-8")

      (str/starts-with? uri "/css/")
      (let [rel (subs uri 5)]
        (serve-static-file (io/file (cfg/public-dir) "css" rel) "text/css"))

      (str/starts-with? uri "/js/")
      (let [rel (subs uri 4)]
        (serve-static-file (io/file (cfg/public-dir) "js" rel) "application/javascript"))

      (str/starts-with? uri "/textures/")
      (let [rel (subs uri 10)
            tex-f (io/file (cfg/public-dir) "textures" rel)]
        (if (.exists tex-f)
          {:status 200
           :headers {"Content-Type" "image/png"
                     "Cache-Control" "public, max-age=86400"}
           :body (io/input-stream tex-f)}
          {:status 404
           :headers {"Content-Type" "text/plain"}
           :body "404 Not Found"}))

      ;; 2. BlueMap 3D Proxy & Redirects
      (or (= uri "/map") (= uri "/bluemap"))
      {:status 301
       :headers {"Location" "/map/"}
       :body ""}

      (or (str/starts-with? uri "/map/")
          (str/starts-with? uri "/bluemap/")
          (str/starts-with? uri "/maps/"))
      (proxy/proxy-bluemap req)

      ;; 3. Skins
      (str/starts-with? uri "/skins/")
      (let [filename (subs uri 7)
            skin-f (io/file (cfg/skins-dir) filename)]
        (if (.exists skin-f)
          {:status 200
           :headers {"Content-Type" "image/png"
                     "Cache-Control" "max-age=3600"}
           :body (io/input-stream skin-f)}
          {:status 200
           :headers {"Content-Type" "image/png"}
           :body (byte-array 0)}))

      ;; 4. APIs: Telemetry & Inventory
      (and (= method :get) (= uri "/api/pos"))
      (let [p (get params "player" "pace")
            out (try (:out (sh/sh "/home/pace/.local/bin/mc" "pos" p))
                     (catch Exception _ "{}"))
            data (try (json/read-str out :key-fn keyword)
                      (catch Exception _ {:ok false :error "parse_error"}))]
        (json-resp data))

      (and (= method :get) (= uri "/api/telemetry"))
      (json-resp (get-telemetry))

      (and (= method :get) (= uri "/api/inventory"))
      (let [p (get params "player" "pace")
            data (get-inventory-response p)]
        (if (:error data)
          (json-resp 404 data)
          (json-resp data)))

      ;; 5. Waypoints
      (and (= method :get) (= uri "/api/waypoints"))
      (let [wp-file (io/file (cfg/config-dir) "waypoints.json")
            wps (if (.exists wp-file) (try (json/read-str (slurp wp-file)) (catch Exception _ [])) [])]
        (json-resp wps))

      (and (= method :post) (= uri "/api/waypoints"))
      (let [body (parse-json-body req)
            wp-file (io/file (cfg/config-dir) "waypoints.json")
            wps (if (.exists wp-file) (try (json/read-str (slurp wp-file)) (catch Exception _ [])) [])
            updated (conj wps body)]
        (spit wp-file (json/write-str updated {:indent true}))
        (json-resp {:ok true :waypoints updated}))

      ;; 6. Gear & Chests
      (and (= method :post) (= uri "/api/gear/repair"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg cnt] (inv/repair-player-gear! player)]
        (json-resp {:ok ok? :message msg :count cnt}))

      (and (= method :post) (= uri "/api/chests/deposit"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg batch] (chests/deposit-player-to-base-chests! player)]
        (json-resp {:ok ok? :message msg :deposited batch}))

      ;; 7. Builds: Custom House
      (and (= method :post) (= uri "/api/build/custom-house"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            wood (get-param params body :wood "cherry")
            w (Integer/parseInt (str (or (get-param params body :width nil) (get-param params body :w 9))))
            l (Integer/parseInt (str (or (get-param params body :length nil) (get-param params body :l 11))))
            h (Integer/parseInt (str (or (get-param params body :height nil) (get-param params body :h 4))))
            dir (or (get-param params body :direction nil) (get-param params body :dir nil))
            [ok? msg] (builds/build-custom-house! player wood w l h dir)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-custom-house"))
      (let [[ok? msg] (builds/undo-custom-house!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      ;; 8. Builds: Thermal Pool & Obelisk
      (and (= method :post) (= uri "/api/build/thermal-pool"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/build-thermal-pool! player)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-thermal-pool"))
      (let [[ok? msg] (builds/undo-thermal-pool!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/enderman-totem"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/build-enderman-totem! player)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-enderman-totem"))
      (let [[ok? msg] (builds/undo-enderman-totem!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      ;; 9. Builds: Pyramid & Purge
      (and (= method :post) (= uri "/api/build/pyramid"))
      (let [body (parse-json-body req)
            cx (Integer/parseInt (str (get-param params body :x -160)))
            cy (Integer/parseInt (str (get-param params body :y 70)))
            cz (Integer/parseInt (str (get-param params body :z -160)))
            radius (Integer/parseInt (str (get-param params body :radius 25)))
            [ok? msg] (builds/build-pyramid! cx cy cz radius)]
        (json-resp {:ok ok? :message msg}))

      ;; 10. Builds: Lava Fill & Drain
      (and (= method :post) (= uri "/api/build/lava"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            radius (Integer/parseInt (str (get-param params body :radius 4)))
            depth (Integer/parseInt (str (get-param params body :depth 4)))
            [ok? msg] (builds/fill-lava! player radius depth)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-lava"))
      (let [[ok? msg] (builds/undo-fill-lava!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      ;; 11. Builds: Stone Castle
      (and (= method :post) (= uri "/api/build/castle"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/build-stone-castle! player)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-castle"))
      (let [[ok? msg] (builds/undo-stone-castle!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      ;; 11b. Builds: Titanic Void Citadel (Qwen Edition)
      (and (= method :post) (= uri "/api/build/titanic-castle"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/build-titanic-castle! player)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/build/undo-titanic-castle"))
      (let [[ok? msg] (builds/undo-titanic-castle!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/monsters/purge-underground"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/purge-underground-monsters! player)]
        (json-resp {:ok ok? :message msg}))

      ;; 10. Nuke Strike
      (and (= method :post) (= uri "/api/nuke/launch"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            [ok? msg] (builds/trigger-nuke-strike! player)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      ;; 11. Blueprints 3D
      (and (= method :post) (= uri "/api/blueprint/backup"))
      (let [[ok? msg meta-data] (blueprint/backup-main-base!)]
        (json-resp {:ok ok? :message msg :meta meta-data}))

      (and (= method :post) (= uri "/api/blueprint/restore"))
      (let [[ok? msg] (blueprint/restore-blueprint!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/blueprint/paste"))
      (let [body (parse-json-body req)
            player (get-param params body :player "pace")
            tmpl (get-param params body :template "base_principal")
            [ok? msg] (blueprint/paste-structure! player tmpl)]
        (json-resp (if ok? 200 500) {:ok ok? :message msg}))

      (and (= method :post) (= uri "/api/blueprint/undo-paste"))
      (let [[ok? msg] (blueprint/undo-paste-structure!)]
        (json-resp (if ok? 200 400) {:ok ok? :message msg}))

      ;; 12. Run command
      (and (= method :post) (= uri "/api/run"))
      (let [body (parse-json-body req)
            cmd (or (get-param params body :cmd nil) (get-param params body :command "") "")
            [ok? out] (bridge/run-command! cmd)]
        (json-resp (if ok? 200 500) {:ok ok? :output out}))

      ;; 13. Vault & Undo Status
      (and (= method :get) (= uri "/api/vault"))
      (json-resp {:backups []})

      (and (= method :get) (= uri "/api/undo-status"))
      (json-resp (builds/get-undo-state))

      ;; 404 Fallback
      :else
      {:status 404
       :headers {"Content-Type" "text/plain; charset=utf-8"}
       :body (str "404 Not Found: " uri)})))

(defonce server (atom nil))

(defn start-server! [& [port]]
  (let [p (or port (cfg/port))]
    (println (str "🚀 Iniciando servidor Clojure Craft Dashboard na porta " p "..."))
    (reset! server (http-server/run-server handler {:port p}))
    (println (str "✅ Servidor Clojure ativo em http://0.0.0.0:" p))
    @server))

(defn stop-server! []
  (when-let [s @server]
    (s :timeout 100)
    (reset! server nil)
    (println "🛑 Servidor Clojure parado.")))

(defn -main [& args]
  (let [port (when (seq args) (Integer/parseInt (first args)))]
    (start-server! port)
    (println "✨ craft.dashboard.server em execução no thread principal.")))
