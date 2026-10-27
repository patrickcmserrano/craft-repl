package com.craft.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;

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
            // 1. Localizar KnotClassLoader
            ClassLoader knotLoader = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                ClassLoader cl = t.getContextClassLoader();
                if (cl != null && cl.getClass().getName().contains("Knot")) {
                    knotLoader = cl;
                    break;
                }
            }

            if (knotLoader == null) {
                System.err.println("[craft-repl] AVISO: KnotClassLoader não encontrado em threads. Usando ContextClassLoader.");
                knotLoader = Thread.currentThread().getContextClassLoader();
            }

            // Define KnotClassLoader na thread atual do Attach Listener
            Thread.currentThread().setContextClassLoader(knotLoader);

            System.out.println("[craft-repl] Target ClassLoader: " + knotLoader.getClass().getName());

            // 2. Acessar KnotClassDelegate para quebrar o sandbox de isolamento
            Field delegateField = knotLoader.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(knotLoader);

            Field vpcsField = delegate.getClass().getDeclaredField("validParentCodeSources");
            vpcsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Path> vpcs = (Set<Path>) vpcsField.get(delegate);

            Method addUrlFwd = knotLoader.getClass().getDeclaredMethod("addUrlFwd", java.net.URL.class);
            addUrlFwd.setAccessible(true);

            // 3. Registrar JARs do Clojure / nREPL
            File jarsListFile = new File("/tmp/clojure_jars.txt");
            if (jarsListFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(jarsListFile));
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    File jar = new File(line);
                    if (jar.exists()) {
                        inst.appendToSystemClassLoaderSearch(new JarFile(jar));
                        Path p = jar.toPath();
                        vpcs.add(p);
                        try {
                            addUrlFwd.invoke(knotLoader, jar.toURI().toURL());
                        } catch (Throwable ignored) {}
                        count++;
                    }
                }
                reader.close();
                System.out.println("[craft-repl] " + count + " dependências registradas no KnotClassLoader!");
            }

            // 4. Registrar diretório src/clj do projeto
            File srcClj = new File("/home/pace/dev/craft-repl/src/clj");
            if (srcClj.exists()) {
                vpcs.add(srcClj.toPath());
                addUrlFwd.invoke(knotLoader, srcClj.toURI().toURL());
                System.out.println("[craft-repl] Diretório src/clj anexado ao classpath do Knot!");
            }

            // 5. Carregar Clojure Runtime (clojure.lang.RT)
            Class<?> rtClass = Class.forName("clojure.lang.RT", true, knotLoader);
            System.out.println("[craft-repl] Clojure runtime carregado com sucesso: " + rtClass.getName());

            // 6. Iniciar nREPL Server
            Class<?> compilerClass = Class.forName("clojure.lang.Compiler", true, knotLoader);
            Method loadMethod = compilerClass.getMethod("load", java.io.Reader.class);

            String cljScript =
                "(try " +
                "  (require 'nrepl.server) " +
                "  (let [start-srv (resolve 'nrepl.server/start-server)] " +
                "    (defonce craft-nrepl-server (start-srv :port " + port + " :bind \"127.0.0.1\")) " +
                "    (println \"[craft-repl] 🚀🚀🚀 nREPL Server escutando em 127.0.0.1:\" " + port + ")) " +
                "  (catch Throwable t " +
                "    (println \"[craft-repl] Erro ao iniciar nREPL:\" (.getMessage t)) " +
                "    (.printStackTrace t)))";

            loadMethod.invoke(null, new StringReader(cljScript));

        } catch (Throwable t) {
            System.err.println("[craft-repl] ❌ Falha durante o bootstrap do agente:");
            t.printStackTrace();
        }
    }
}
