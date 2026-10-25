package com.craft.agent;

import java.io.File;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * CraftAgent - Dynamic JVM Attach Agent that bootstraps Clojure & nREPL
 * inside the Minecraft / Fabric runtime (KnotClassLoader).
 */
public class CraftAgent {

    private static final int DEFAULT_PORT = 7888;

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("=================================================");
        System.out.println("[craft-repl] Agente acoplado com sucesso via Attach API!");
        System.out.println("=================================================");

        int port = DEFAULT_PORT;
        if (agentArgs != null && !agentArgs.trim().isEmpty()) {
            try {
                port = Integer.parseInt(agentArgs.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            // 1. Localizar o ClassLoader do Fabric (Knot) ou do Servidor
            ClassLoader knotLoader = null;
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
                    knotLoader = clazz.getClassLoader();
                    break;
                }
                if (clazz.getName().equals("net.minecraft.server.MinecraftServer")) {
                    knotLoader = clazz.getClassLoader();
                    break;
                }
            }

            if (knotLoader == null) {
                System.err.println("[craft-repl] AVISO: KnotClassLoader não encontrado diretamente. Usando ContextClassLoader.");
                knotLoader = Thread.currentThread().getContextClassLoader();
            }

            System.out.println("[craft-repl] Target ClassLoader: " + knotLoader.getClass().getName());

            // 2. Tentar carregar clojure.lang.RT no KnotClassLoader
            Class<?> rtClass = null;
            try {
                rtClass = Class.forName("clojure.lang.RT", true, knotLoader);
            } catch (ClassNotFoundException e) {
                System.out.println("[craft-repl] Clojure runtime ainda não visível no KnotClassLoader.");
            }

            // 3. Inicializar e iniciar nREPL server
            if (rtClass != null) {
                Class<?> compilerClass = Class.forName("clojure.lang.Compiler", true, knotLoader);
                Method load = compilerClass.getMethod("load", java.io.Reader.class);

                String cljScript =
                    "(try " +
                    "  (require '[nrepl.server :as nrepl]) " +
                    "  (defonce craft-nrepl-server (nrepl/start-server :port " + port + " :bind \"127.0.0.1\")) " +
                    "  (println \"[craft-repl] 🚀 nREPL escutando em 127.0.0.1:" + port + "\") " +
                    "  (catch Throwable t " +
                    "    (println \"[craft-repl] Erro ao iniciar nREPL:\" (.getMessage t))))";

                load.invoke(null, new StringReader(cljScript));
            } else {
                System.out.println("[craft-repl] Dica: Certifique-se de que o JAR do Clojure e do nREPL foram adicionados ao classpath.");
            }

        } catch (Throwable t) {
            System.err.println("[craft-repl] Falha durante o bootstrap:");
            t.printStackTrace();
        }
    }
}
