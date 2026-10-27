package com.craft.patch;

import java.io.File;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarFile;

public class ClojureBootstrapperAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("=================================================");
        System.out.println("[craft-repl] 🚀 Inicializando Runtime Clojure & nREPL...");
        System.out.println("=================================================");

        try {
            File configFile = new File(args != null && !args.trim().isEmpty() ? args.trim() : "/tmp/clojure_jars.txt");
            if (!configFile.exists()) {
                System.err.println("[craft-repl] ❌ Arquivo de configuração não encontrado: " + configFile);
                return;
            }

            List<String> jars = Files.readAllLines(configFile.toPath());
            for (String jarPath : jars) {
                jarPath = jarPath.trim();
                if (jarPath.isEmpty()) continue;
                File f = new File(jarPath);
                if (f.exists() && f.getName().endsWith(".jar")) {
                    try {
                        inst.appendToSystemClassLoaderSearch(new JarFile(f));
                    } catch (Exception e) {
                        System.err.println("[craft-repl] Falha ao anexar: " + jarPath + " (" + e.getMessage() + ")");
                    }
                }
            }
            System.out.println("[craft-repl] ✅ " + jars.size() + " dependências do Clojure anexadas ao SystemClassLoader!");

            // 2. Localizar o KnotClassLoader do Fabric
            Class<?> fabricLoaderClass = null;
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals("net.fabricmc.loader.api.FabricLoader")) {
                    fabricLoaderClass = clazz;
                    break;
                }
            }

            if (fabricLoaderClass == null) {
                System.err.println("[craft-repl] ❌ FabricLoader não localizado!");
                return;
            }

            Object fabricInstance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object gameInstance = fabricLoaderClass.getMethod("getGameInstance").invoke(fabricInstance);
            ClassLoader knotLoader = gameInstance.getClass().getClassLoader();

            System.out.println("[craft-repl] Target ClassLoader: " + knotLoader);
            Thread.currentThread().setContextClassLoader(knotLoader);

            // 3. Inicializar Clojure RT
            Class<?> rtClass = Class.forName("clojure.lang.RT", true, knotLoader);
            System.out.println("[craft-repl] clojure.lang.RT inicializado com sucesso: " + rtClass);

            // 4. Bootstrapar nREPL Server na porta 7888
            Class<?> compilerClass = Class.forName("clojure.lang.Compiler", true, knotLoader);
            Method loadMethod = compilerClass.getMethod("load", java.io.Reader.class);

            String bootstrapScript = 
                "(try " +
                "  (require '[nrepl.server :as nrepl]) " +
                "  (defonce craft-nrepl (nrepl/start-server :port 7888 :bind \"127.0.0.1\")) " +
                "  (println \"[craft-repl] 🔥 nREPL ONLINE em 127.0.0.1:7888\") " +
                "  (try " +
                "    (require '[craft.core :refer [in-server-thread]]) " +
                "    (require '[craft.player :as p]) " +
                "    (require '[craft.world :as w]) " +
                "    (println \"[craft-repl] 🧩 Namespaces [craft.core, craft.player, craft.world] carregados!\") " +
                "    (catch Throwable e (println \"[craft-repl] Aviso ao carregar craft.*:\" (.getMessage e)))) " +
                "  (catch Throwable t " +
                "    (println \"[craft-repl] ❌ Erro ao subir nREPL:\" (.getMessage t))))";

            loadMethod.invoke(null, new StringReader(bootstrapScript));
            System.out.println("[craft-repl] ✅ Bootstrap do Clojure finalizado com sucesso!");

        } catch (Throwable t) {
            System.err.println("[craft-repl] ❌ Falha crítica no bootstrap do Clojure:");
            t.printStackTrace();
        }
    }
}
