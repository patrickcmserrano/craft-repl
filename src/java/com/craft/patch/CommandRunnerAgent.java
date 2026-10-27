package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;

public class CommandRunnerAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[CommandRunnerAgent] 🚀 Disparando teleporte direto no console do servidor...");
        try {
            Class<?> fabricLoaderClass = null;
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals("net.fabricmc.loader.api.FabricLoader")) {
                    fabricLoaderClass = clazz;
                    break;
                }
            }
            if (fabricLoaderClass == null) return;

            Object fabricInstance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object gameInstance = fabricLoaderClass.getMethod("getGameInstance").invoke(fabricInstance);

            Object server = null;
            for (Method m : gameInstance.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getServer") || m.getName().equals("method_1562") || m.getReturnType().getName().contains("1132"))) {
                    server = m.invoke(gameInstance);
                    if (server != null) break;
                }
            }

            if (server == null) {
                System.err.println("[CommandRunnerAgent] Servidor não encontrado.");
                return;
            }

            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);

            final String cmd = (args != null && !args.trim().isEmpty()) ? args.trim() : "tp Lizzie pace";

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
                        System.out.println("[CommandRunnerAgent] Executando comando de console: " + cmd);
                        for (Method m : commandManager.getClass().getMethods()) {
                            if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == String.class) {
                                if (m.getName().equals("executeWithPrefix") || m.getName().equals("method_9249") || m.getParameterTypes()[0].isAssignableFrom(commandSource.getClass())) {
                                    try {
                                        m.invoke(commandManager, commandSource, cmd);
                                        System.out.println("[CommandRunnerAgent] ✅ Comando '" + cmd + "' executado via CommandManager!");
                                        break;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    } else {
                        System.err.println("[CommandRunnerAgent] Não foi possível obter commandSource ou commandManager.");
                    }

                    // 3. Fallback: movimentação física direta no objeto Entity
                    Object playerManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                            playerManager = m.invoke(fServer);
                            if (playerManager != null) break;
                        }
                    }

                    if (playerManager != null) {
                        List<?> players = null;
                        for (Method m : playerManager.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                                players = (List<?>) m.invoke(playerManager);
                                if (players != null) break;
                            }
                        }

                        Object host = null;
                        Object guest = null;
                        if (players != null) {
                            for (Object p : players) {
                                String s = p.toString().toLowerCase();
                                if (s.contains("pace")) host = p;
                                else guest = p;
                            }
                        }

                        if (host != null && guest != null) {
                            double x = (double) host.getClass().getMethod("getX").invoke(host);
                            double y = (double) host.getClass().getMethod("getY").invoke(host);
                            double z = (double) host.getClass().getMethod("getZ").invoke(host);

                            System.out.println(String.format("[CommandRunnerAgent] Pos do host: %.1f, %.1f, %.1f", x, y, z));

                            // requestTeleport(x, y, z) ou setPosition(x, y, z)
                            for (Method m : guest.getClass().getMethods()) {
                                if (m.getParameterCount() == 3 && m.getParameterTypes()[0] == double.class) {
                                    if (m.getName().equals("requestTeleport") || m.getName().equals("method_14279") || m.getName().equals("setPosition")) {
                                        m.invoke(guest, x, y + 0.5, z);
                                        System.out.println("[CommandRunnerAgent] 🚀 Fallback requestTeleport/setPosition executado em " + m.getName() + "!");
                                        break;
                                    }
                                }
                            }

                            // Reenviar commandTree para o host ter comandos no chat
                            if (commandManager != null) {
                                for (Method m : commandManager.getClass().getMethods()) {
                                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(host.getClass())) {
                                        m.invoke(commandManager, host);
                                        System.out.println("[CommandRunnerAgent] 🌳 CommandTree reenviada para 'pace'!");
                                        break;
                                    }
                                }
                            }
                        }
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
