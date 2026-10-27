package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;

public class LiveTeleportAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[LiveTeleportAgent] ⚡ Injetado na JVM!");
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

            // Localizar o servidor
            Object server = null;
            for (Method m : gameInstance.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    String name = m.getName();
                    String ret = m.getReturnType().getName();
                    if (name.equals("getServer") || name.equals("method_1562") || ret.contains("Server") || ret.contains("class_1132")) {
                        try {
                            Object res = m.invoke(gameInstance);
                            if (res != null) {
                                server = res;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (server == null) {
                System.err.println("[LiveTeleportAgent] Servidor não encontrado!");
                return;
            }

            final Object fServer = server;

            // Procurar método de execução no ServerThread
            Method runMethod = null;
            for (Method m : fServer.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                    runMethod = m;
                    break;
                }
            }

            if (runMethod == null) return;

            runMethod.invoke(fServer, (Runnable) () -> {
                try {
                    // Obter PlayerManager
                    Object playerManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("PlayerManager") || m.getReturnType().getName().contains("class_3324"))) {
                            playerManager = m.invoke(fServer);
                            if (playerManager != null) break;
                        }
                    }

                    if (playerManager == null) return;

                    // Obter lista de jogadores online
                    List<?> players = null;
                    for (Method m : playerManager.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                            Object res = m.invoke(playerManager);
                            if (res instanceof List) {
                                players = (List<?>) res;
                                break;
                            }
                        }
                    }

                    System.out.println("[LiveTeleportAgent] Jogadores online encontrados: " + (players != null ? players.size() : 0));

                    Object targetHost = null;
                    Object targetGuest = null;

                    if (players != null) {
                        for (Object p : players) {
                            String name = "";
                            for (Method m : p.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("method_5477") || m.getName().equals("method_5820"))) {
                                    Object n = m.invoke(p);
                                    if (n != null) name = n.toString();
                                    break;
                                }
                            }
                            System.out.println("[LiveTeleportAgent] Jogador na lista: '" + name + "'");
                            if (name.toLowerCase().contains("pace")) {
                                targetHost = p;
                            } else {
                                targetGuest = p;
                            }
                        }
                    }

                    if (targetHost == null || targetGuest == null) {
                        System.err.println("[LiveTeleportAgent] Não foi possível identificar host e convidado.");
                        return;
                    }

                    // Obter posição do host
                    double x = 0, y = 0, z = 0;
                    float yaw = 0, pitch = 0;
                    Object world = null;

                    for (Method m : targetHost.getClass().getMethods()) {
                        if (m.getParameterCount() == 0) {
                            if (m.getName().equals("getX")) x = (double) m.invoke(targetHost);
                            if (m.getName().equals("getY")) y = (double) m.invoke(targetHost);
                            if (m.getName().equals("getZ")) z = (double) m.invoke(targetHost);
                            if (m.getName().equals("getYaw")) yaw = (float) m.invoke(targetHost);
                            if (m.getName().equals("getPitch")) pitch = (float) m.invoke(targetHost);
                            if (m.getName().equals("getWorld") || m.getName().equals("method_37908")) world = m.invoke(targetHost);
                        }
                    }

                    System.out.println(String.format("[LiveTeleportAgent] Posição do Host: [%.1f, %.1f, %.1f]", x, y, z));

                    // Teleportar convidado para o host
                    // teleport(ServerWorld world, double x, double y, double z, float yaw, float pitch)
                    boolean teleported = false;
                    for (Method m : targetGuest.getClass().getMethods()) {
                        if (m.getName().equals("teleport") && m.getParameterCount() == 6) {
                            m.invoke(targetGuest, world, x, y, z, yaw, pitch);
                            teleported = true;
                            System.out.println("[LiveTeleportAgent] 🚀 SUCESSO! Teleporte concluído via teleport(world, x, y, z, yaw, pitch)!");
                            break;
                        }
                    }

                    // Também dar OP para o pace para ele poder usar comandos sempre
                    try {
                        Object gameProfile = null;
                        for (Method m : targetHost.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getGameProfile") || m.getName().equals("method_5678") || m.getReturnType().getName().contains("GameProfile"))) {
                                gameProfile = m.invoke(targetHost);
                                break;
                            }
                        }

                        if (gameProfile != null) {
                            for (Method m : playerManager.getClass().getMethods()) {
                                if (m.getParameterCount() == 1 && m.getParameterTypes()[0].getName().contains("GameProfile")) {
                                    if (m.getName().equals("addToOperators") || m.getName().equals("method_14561")) {
                                        m.invoke(playerManager, gameProfile);
                                        System.out.println("[LiveTeleportAgent] 👑 OP concedido ao Host (comandos de chat liberados permanentemente)!");
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception opEx) {
                        System.err.println("[LiveTeleportAgent] Aviso ao conceder OP: " + opEx.getMessage());
                    }

                } catch (Exception e) {
                    System.err.println("[LiveTeleportAgent] Erro dentro do ServerThread:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("[LiveTeleportAgent] Falha geral:");
            e.printStackTrace();
        }
    }
}
