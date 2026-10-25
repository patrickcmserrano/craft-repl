package com.craft.agent;

import com.sun.tools.attach.VirtualMachine;
import java.io.File;

/**
 * Injector - Dispara a conexão com a JVM alvo e anexa o craft-repl-agent.jar.
 */
public class Injector {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java com.craft.agent.Injector <PID> <PATH_TO_AGENT_JAR> [PORT]");
            System.exit(1);
        }

        String pid = args[0];
        String agentJarPath = args[1];
        String port = args.length > 2 ? args[2] : "7888";

        File jarFile = new File(agentJarPath);
        if (!jarFile.exists()) {
            System.err.println("Erro: Arquivo do agente não encontrado: " + agentJarPath);
            System.exit(1);
        }

        try {
            System.out.println("[craft-repl] Conectando à JVM com PID " + pid + "...");
            VirtualMachine vm = VirtualMachine.attach(pid);
            System.out.println("[craft-repl] Injetando agente: " + jarFile.getAbsolutePath() + " (porta: " + port + ")");
            vm.loadAgent(jarFile.getAbsolutePath(), port);
            vm.detach();
            System.out.println("[craft-repl] ✅ Injeção concluída com sucesso! Conecte-se via nREPL na porta " + port);
        } catch (Exception e) {
            System.err.println("[craft-repl] ❌ Erro ao anexar agente:");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
