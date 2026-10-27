package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;

public class LiveGodAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[LiveGodAgent] 👑 Iniciando liberação divina...");
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
                System.err.println("[LiveGodAgent] Servidor não encontrado.");
                return;
            }

            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    // 1. Obter PlayerManager
                    Object playerManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("PlayerManager") || m.getReturnType().getName().contains("3324"))) {
                            playerManager = m.invoke(fServer);
                            if (playerManager != null) break;
                        }
                    }

                    if (playerManager == null) return;

                    // 2. ATIVAR CHEATS / COMANDOS NO MUNDO LAN
                    for (Method m : playerManager.getClass().getMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                            if (m.getName().toLowerCase().contains("cheat") || m.getName().equals("setCheatsAllowed")) {
                                m.invoke(playerManager, true);
                                System.out.println("[LiveGodAgent] ✅ Comandos/Cheats liberados no PlayerManager!");
                            }
                        }
                    }

                    // 3. Obter lista de jogadores e teleportar
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

                    Object host = null;
                    Object guest = null;

                    if (players != null) {
                        for (Object p : players) {
                            String name = "";
                            for (Method m : p.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("method_5477"))) {
                                    Object n = m.invoke(p);
                                    if (n != null) name = n.toString();
                                    break;
                                }
                            }
                            System.out.println("[LiveGodAgent] Jogador online: " + name);
                            if (name.toLowerCase().contains("pace")) {
                                host = p;
                            } else {
                                guest = p;
                            }
                        }
                    }

                    if (host != null) {
                        // Dar OP ao host
                        Object gameProfile = null;
                        for (Method m : host.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getGameProfile") || m.getReturnType().getName().contains("GameProfile"))) {
                                gameProfile = m.invoke(host);
                                break;
                            }
                        }
                        if (gameProfile != null) {
                            for (Method m : playerManager.getClass().getMethods()) {
                                if (m.getParameterCount() == 1 && m.getParameterTypes()[0].getName().contains("GameProfile")) {
                                    m.invoke(playerManager, gameProfile);
                                    System.out.println("[LiveGodAgent] 👑 Permissão OP concedida a 'pace'!");
                                    break;
                                }
                            }
                        }
                    }

                    if (host != null && guest != null) {
                        double x = 0, y = 0, z = 0;
                        float yaw = 0, pitch = 0;
                        Object world = null;

                        for (Method m : host.getClass().getMethods()) {
                            if (m.getParameterCount() == 0) {
                                if (m.getName().equals("getX")) x = (double) m.invoke(host);
                                if (m.getName().equals("getY")) y = (double) m.invoke(host);
                                if (m.getName().equals("getZ")) z = (double) m.invoke(host);
                                if (m.getName().equals("getYaw")) yaw = (float) m.invoke(host);
                                if (m.getName().equals("getPitch")) pitch = (float) m.invoke(host);
                                if (m.getName().equals("getWorld") || m.getName().equals("method_37908")) world = m.invoke(host);
                            }
                        }

                        for (Method m : guest.getClass().getMethods()) {
                            if (m.getName().equals("teleport") && m.getParameterCount() == 6) {
                                m.invoke(guest, world, x, y, z, yaw, pitch);
                                System.out.println(String.format("[LiveGodAgent] 🚀 Lizzie teleportada para [%.1f, %.1f, %.1f]!", x, y, z));
                                break;
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[LiveGodAgent] Erro na execução:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("[LiveGodAgent] Falha:");
            e.printStackTrace();
        }
    }
}
