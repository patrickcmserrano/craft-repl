package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CommandRunnerAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[CommandRunnerAgent] 🚀 Executando comando no servidor...");
        try {
            Object server = null;

            // 1. Tenta obter via VoiceChatServerThread ou threads ativas
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("VoiceChatServerThread") || t.getName().contains("Server thread")) {
                    for (Field f : t.getClass().getDeclaredFields()) {
                        String typeName = f.getType().getName();
                        if (typeName.contains("3176") || typeName.contains("1132") || typeName.contains("MinecraftServer") || typeName.contains("DedicatedServer")) {
                            f.setAccessible(true);
                            server = f.get(t);
                            if (server != null) break;
                        }
                    }
                }
                if (server != null) break;
            }

            // 2. Fallback: FabricLoader getGameInstance (para cliente Singleplayer)
            if (server == null) {
                Class<?> fabricLoaderClass = null;
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (clazz.getName().equals("net.fabricmc.loader.api.FabricLoader")) {
                        fabricLoaderClass = clazz;
                        break;
                    }
                }
                if (fabricLoaderClass != null) {
                    Object fabricInstance = fabricLoaderClass.getMethod("getInstance").invoke(null);
                    Object gameInstance = fabricLoaderClass.getMethod("getGameInstance").invoke(fabricInstance);
                    if (gameInstance != null) {
                        for (Method m : gameInstance.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getServer") || m.getName().equals("method_1576") || m.getReturnType().getName().contains("1132"))) {
                                server = m.invoke(gameInstance);
                                if (server != null) break;
                            }
                        }
                    }
                }
            }

            if (server == null) {
                System.err.println("[CommandRunnerAgent] Servidor não encontrado.");
                return;
            }

            System.out.println("[CommandRunnerAgent] Servidor localizado: " + server.getClass().getName());

            final Object fServer = server;
            Method executeMethod = null;
            for (Method m : fServer.getClass().getMethods()) {
                if (m.getName().equals("execute") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                    executeMethod = m;
                    break;
                }
            }
            if (executeMethod == null) {
                System.err.println("[CommandRunnerAgent] executeMethod não encontrado.");
                return;
            }

            final String cmd = (args != null && !args.trim().isEmpty()) ? args.trim() : "say Servidor Ativo!";

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    // 1. Obter ServerCommandSource
                    Object commandSource = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getCommandSource") || m.getReturnType().getName().contains("class_2168") || m.getReturnType().getName().contains("ServerCommandSource"))) {
                            commandSource = m.invoke(fServer);
                            if (commandSource != null) break;
                        }
                    }

                    // 2. Obter CommandManager
                    Object commandManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getCommandManager") || m.getReturnType().getName().contains("class_2170") || m.getReturnType().getName().contains("CommandManager"))) {
                            commandManager = m.invoke(fServer);
                            if (commandManager != null) break;
                        }
                    }

                    if (commandSource != null && commandManager != null) {
                        String cleanCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                        for (Method m : commandManager.getClass().getMethods()) {
                            if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == String.class) {
                                if (m.getName().equals("executeWithPrefix") || m.getName().equals("method_9249") || m.getParameterTypes()[0].isAssignableFrom(commandSource.getClass())) {
                                    try {
                                        m.invoke(commandManager, commandSource, cleanCmd);
                                        System.out.println("[CommandRunnerAgent] ✅ Comando executado: /" + cleanCmd);
                                        break;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    } else {
                        System.err.println("[CommandRunnerAgent] Não foi possível obter commandSource ou commandManager.");
                    }
                } catch (Exception e) {
                    System.err.println("[CommandRunnerAgent] Erro na execução:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("[CommandRunnerAgent] Falha:");
            e.printStackTrace();
        }
    }
}
