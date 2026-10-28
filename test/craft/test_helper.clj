(ns craft.test-helper
  "HTTP Client and Server Process Harness for safe testing."
  (:require [craft.fixtures :as fix]
            [clojure.data.json :as json])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.time Duration]))

(def ^:dynamic *server-port* 18090)
(def ^:dynamic *sandbox* nil)

(def ^:private http-client
  (-> (HttpClient/newBuilder)
      (.connectTimeout (Duration/ofSeconds 4))
      (.build)))

(defn start-sandbox-server!
  "Starts Clojure or Python server in sandbox mode on an ephemeral port."
  [port sandbox]
  (let [backend (or (System/getenv "MC_BACKEND") "clj")
        java-bin (str (System/getProperty "java.home") "/bin/java")
        cmd-args (if (= backend "py")
                   ["/usr/bin/python3" "/home/pace/mc-dashboard/server.py"]
                   [java-bin "-cp" (System/getProperty "java.class.path") "clojure.main" "-m" "craft.dashboard.server" (str port)])
        pb (ProcessBuilder. ^java.util.List cmd-args)
        env (.environment pb)]
    (.put env "MC_PORT" (str port))
    (.put env "MC_DRY_RUN" "1")
    (.put env "MC_FAST_TIMERS" "1")
    (.put env "MC_CONFIG_DIR" (str (:config-dir sandbox)))
    (.put env "MC_SAVES_DIR" (str (:saves-dir sandbox)))
    (.put env "MC_LOG_FILE" (str (clojure.java.io/file (:logs-dir sandbox) "latest.log")))
    (.put env "MC_SKINS_DIR" (str (:skins-dir sandbox)))
    (.put env "MC_WORLD_DIR" (str (:world-dir sandbox)))
    (.put env "MC_MCA_PATH" (str (clojure.java.io/file (:region-dir sandbox) "r.-1.-1.mca")))
    (.put env "MC_BLUEPRINT_OUT_DIR" (str (:blueprints-dir sandbox)))
    (.put env "MC_BLUEPRINT_PATH" (str (clojure.java.io/file (:blueprints-dir sandbox) "base_principal_latest.nbt")))
    (.put env "MC_PUBLIC_DIR" "/home/pace/dev/craft-repl/resources/public")
    (.redirectOutput pb (java.lang.ProcessBuilder$Redirect/INHERIT))
    (.redirectError pb (java.lang.ProcessBuilder$Redirect/INHERIT))
    (let [proc (.start pb)]
      ;; Wait for server to listen
      (loop [attempts 0]
        (if (> attempts 50)
          (do (.destroyForcibly proc)
              (throw (RuntimeException. "Sandbox server failed to start within timeout")))
          (let [ready? (try
                         (let [req (-> (HttpRequest/newBuilder)
                                       (.uri (URI/create (str "http://127.0.0.1:" port "/api/telemetry")))
                                       (.GET)
                                       (.timeout (Duration/ofMillis 300))
                                       (.build))
                               resp (.send http-client req (HttpResponse$BodyHandlers/ofString))]
                           (= 200 (.statusCode resp)))
                         (catch Exception _ false))]
            (if ready?
              proc
              (do (Thread/sleep 150)
                  (recur (inc attempts))))))))))

(defn http-get
  ([path] (http-get *server-port* path))
  ([port path]
   (let [req (-> (HttpRequest/newBuilder)
                 (.uri (URI/create (str "http://127.0.0.1:" port path)))
                 (.GET)
                 (.timeout (Duration/ofSeconds 5))
                 (.build))
         resp (.send http-client req (HttpResponse$BodyHandlers/ofString))
         status (.statusCode resp)
         body (.body resp)
         json-data (try (json/read-str body :key-fn keyword) (catch Exception _ body))]
     {:status status :body json-data :raw body :headers (.map (.headers resp))})))

(defn http-post
  ([path] (http-post *server-port* path nil))
  ([path body-data] (http-post *server-port* path body-data))
  ([port path body-data]
   (let [body-str (if (string? body-data) body-data (json/write-str (or body-data {})))
         builder (-> (HttpRequest/newBuilder)
                     (.uri (URI/create (str "http://127.0.0.1:" port path)))
                     (.POST (HttpRequest$BodyPublishers/ofString body-str))
                     (.header "Content-Type" "application/json")
                     (.timeout (Duration/ofSeconds 5)))
         req (.build builder)
         resp (.send http-client req (HttpResponse$BodyHandlers/ofString))
         status (.statusCode resp)
         body (.body resp)
         json-data (try (json/read-str body :key-fn keyword) (catch Exception _ body))]
     {:status status :body json-data :raw body :headers (.map (.headers resp))})))

(defmacro with-sandbox-server [& body]
  `(let [sandbox# (fix/create-sandbox!)
         port# (+ 18000 (rand-int 1000))
         proc# (start-sandbox-server! port# sandbox#)]
     (try
       (binding [*sandbox* sandbox#
                 *server-port* port#]
         ~@body)
       (finally
         (.destroyForcibly proc#)))))
