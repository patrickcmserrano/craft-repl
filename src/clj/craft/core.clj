(ns craft.core
  "Núcleo de interop e controle do runtime do Minecraft (Fabric 1.21.1).
   Suporta execução segura no ServerThread e comandos de console."
  (:import [java.lang.reflect Method]
           [java.util.concurrent Executor]))

(defonce ^:private cached-server (atom nil))

(defn find-minecraft-server
  "Localiza a instância ativa do MinecraftServer no runtime Fabric."
  []
  (if-let [srv @cached-server]
    srv
    (let [knot-loader (or (.getContextClassLoader (Thread/currentThread))
                          (.getClassLoader (class (find-minecraft-server))))]
      (try
        (let [fabric-class (Class/forName "net.fabricmc.loader.api.FabricLoader" true knot-loader)
              get-instance (.getMethod fabric-class "getInstance" (into-array Class []))
              fabric-inst  (.invoke get-instance nil (into-array Object []))
              get-game     (.getMethod fabric-class "getGameInstance" (into-array Class []))
              game-inst    (.invoke get-game fabric-inst (into-array Object []))
              game-cls     (.getClass game-inst)
              ;; Procura método getServer() ou method_1576()
              server-method (or (try (.getMethod game-cls "getServer" (into-array Class [])) (catch Throwable _ nil))
                                (try (.getMethod game-cls "method_1576" (into-array Class [])) (catch Throwable _ nil))
                                (some (fn [^Method m]
                                        (let [rt (.getReturnType m)]
                                          (when (and (= 0 (count (.getParameterTypes m)))
                                                     (or (.contains (.getName rt) "class_1132")
                                                         (.contains (.getName rt) "MinecraftServer")))
                                            m)))
                                      (.getMethods game-cls)))
              server        (when server-method (.invoke server-method game-inst (into-array Object [])))]
          (when server
            (reset! cached-server server))
          server)
        (catch Throwable t
          (println "[craft.core] Falha ao localizar servidor via FabricLoader:" (.getMessage t))
          nil)))))

(defmacro in-server-thread
  "Executa o corpo dentro da thread do servidor (ServerThread).
   Garante a segurança de concorrência com o tick loop do Minecraft."
  [& body]
  `(let [server# (find-minecraft-server)]
     (if (nil? server#)
       (throw (IllegalStateException. "MinecraftServer não encontrado! O mundo está carregado?"))
       (let [^Executor exec# (cast Executor server#)]
         (.execute exec#
                   (reify Runnable
                     (run [this]
                       (try
                         ~@body
                         (catch Throwable t#
                           (println "[craft.core] Erro na execução no ServerThread:"
                                    (.getMessage t#))))))))))

(defn run-command!
  "Executa um comando de console no servidor Minecraft."
  [cmd]
  (in-server-thread
    (let [server (find-minecraft-server)
          cmd-mgr (or (try (.getCommandManager server) (catch Throwable _ nil))
                      (try (let [m (some #(when (re-find #"CommandManager|class_2170" (str (.getReturnType %))) %)
                                         (.getMethods (.getClass server)))]
                             (.invoke m server (into-array Object [])))
                           (catch Throwable _ nil)))
          cmd-src (or (try (.getCommandSource server) (catch Throwable _ nil))
                      (try (let [m (some #(when (re-find #"ServerCommandSource|class_2168" (str (.getReturnType %))) %)
                                         (.getMethods (.getClass server)))]
                             (.invoke m server (into-array Object [])))
                           (catch Throwable _ nil)))
          clean-cmd (if (.startsWith cmd "/") (.substring cmd 1) cmd)]
      (when (and cmd-mgr cmd-src)
        ;; Invoca executeWithPrefix ou method_9249 / method_54316
        (let [exec-m (some (fn [^Method m]
                             (let [p (.getParameterTypes m)]
                               (when (and (= 2 (count p))
                                          (or (.isAssignableFrom (first p) (.getClass cmd-src))
                                              (.contains (.getName (first p)) "2168")
                                              (.contains (.getName (first p)) "ServerCommandSource"))
                                          (= String (second p)))
                                 m)))
                           (.getMethods (.getClass cmd-mgr)))]
          (when exec-m
            (.invoke exec-m cmd-mgr (into-array Object [cmd-src clean-cmd]))
            (println (str "[craft.core] Comando executado: /" clean-cmd))))))))
