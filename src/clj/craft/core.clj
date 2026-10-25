(ns craft.core
  "Núcleo de interop e controle do runtime do Minecraft.
   Fornece descoberta do servidor e a macro essencial in-server-thread."
  (:import [java.lang.reflect Method]))

(defonce ^:private cached-server (atom nil))

(defn find-minecraft-server
  "Localiza a instância ativa do MinecraftServer no runtime."
  []
  (if-let [srv @cached-server]
    srv
    (let [knot-loader (or (.getContextClassLoader (Thread/currentThread))
                          (.getClassLoader (class (find-minecraft-server))))]
      (try
        ;; Tenta obter via FabricLoader
        (let [fabric-class (Class/forName "net.fabricmc.loader.api.FabricLoader" true knot-loader)
              get-instance (.getMethod fabric-class "getInstance" (into-array Class []))
              fabric-inst  (.invoke get-instance nil (into-array Object []))
              get-game     (.getMethod fabric-class "getGameInstance" (into-array Class []))
              game-inst    (.invoke get-game fabric-inst (into-array Object []))
              get-server   (.getMethod (.getClass game-inst) "getServer" (into-array Class []))
              server       (.invoke get-server game-inst (into-array Object []))]
          (reset! cached-server server)
          server)
        (catch Throwable t
          (println "[craft.core] Falha ao localizar servidor via FabricLoader:" (.getMessage t))
          nil)))))

(defmacro in-server-thread
  "Executa o corpo dentro da thread do servidor (ServerThread).
   Garante a segurança de concorrência com o tick loop do Minecraft."
  [& body]
  `(let [server# (or @cached-server (find-minecraft-server))]
     (if (nil? server#)
       (throw (IllegalStateException. "MinecraftServer não encontrado! O mundo está carregado?"))
       (let [execute-method# (.getMethod (.getClass server#) "execute" (into-array Class [Runnable]))]
         (.invoke execute-method# server#
                  (into-array Object
                              [(reify Runnable
                                 (run [this]
                                   (try
                                     ~@body
                                     (catch Throwable t#
                                       (println "[craft.core] Erro na execução no ServerThread:"
                                                (.getMessage t#))))))]))))))
