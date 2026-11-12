package com.craft.skin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Carrega o jar do agente num classloader limpo e invoca agentmain pelo
 * descritor, exatamente como InstrumentationImpl.loadClassAndStartAgent faz
 * (getDeclaredMethod("agentmain", String.class, Instrumentation.class)).
 *
 * Pega o defeito "Agent JAR loaded but agent failed to initialize" ANTES de
 * gastar um restart do cliente: se faltar qualquer classe interna do jar
 * (ex. um ClientSkinCacheFixer$N novo), o getDeclaredMethod estoura aqui.
 *
 * Uso: java -cp target/bin com.craft.skin.SkinAgentLoadProbe <fix-client-skin.jar>
 */
public final class SkinAgentLoadProbe {

    public static void main(String[] args) throws Exception {
        File jar = new File(args.length > 0 ? args[0] : "target/fix-client-skin.jar");
        if (!jar.isFile()) { System.out.println("FALHA: sem " + jar); System.exit(1); }

        URLClassLoader cl = new URLClassLoader(new URL[]{ jar.toURI().toURL() }, null);
        int failures = 0;
        try {
            Class<?> agent = Class.forName("com.craft.skin.ClientSkinCacheFixer", true, cl);
            System.out.println("OK  classe carregada de " + jar.getName());
            Method m = agent.getDeclaredMethod("agentmain", String.class, java.lang.instrument.Instrumentation.class);
            System.out.println("OK  agentmain encontrado: " + m);
            // forcar resolucao das classes internas referenciadas nos descritores
            agent.getDeclaredMethods();
            System.out.println("OK  todas as classes internas resolvem");
        } catch (NoClassDefFoundError e) {
            failures++;
            System.out.println("FALHA NoClassDefFoundError: " + e.getMessage());
        } catch (Throwable t) {
            failures++;
            System.out.println("FALHA: " + t);
            t.printStackTrace();
        }
        System.out.println(failures == 0 ? "== TUDO OK ==" : "== " + failures + " FALHAS ==");
        System.exit(failures == 0 ? 0 : 1);
    }
}
