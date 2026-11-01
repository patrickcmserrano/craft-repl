package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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

            // Intercepta comando de telemetria / posição ao vivo sem passar pelo chat
            if (cmd.startsWith("pos")) {
                final String targetName = cmd.length() > 4 ? cmd.substring(4).trim() : "pace";
                executeMethod.invoke(fServer, (Runnable) () -> {
                    try {
                        Object playerManager = null;
                        for (Method m : fServer.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324") || m.getReturnType().getName().contains("PlayerManager"))) {
                                playerManager = m.invoke(fServer);
                                if (playerManager != null) break;
                            }
                        }
                        if (playerManager != null) {
                            List<?> players = null;
                            for (Method m : playerManager.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                                    players = (List<?>) m.invoke(playerManager);
                                    break;
                                }
                            }
                            if (players != null) {
                                for (Object p : players) {
                                    String name = "";
                                    for (Method m : p.getClass().getMethods()) {
                                        if (m.getParameterCount() == 0 && (m.getName().equals("getGameProfile") || m.getReturnType().getName().contains("GameProfile"))) {
                                            Object prof = m.invoke(p);
                                            if (prof != null) {
                                                name = (String) prof.getClass().getMethod("getName").invoke(prof);
                                                break;
                                            }
                                        }
                                    }
                                    if (name.isEmpty()) {
                                        for (Method m : p.getClass().getMethods()) {
                                            if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("method_5477"))) {
                                                Object n = m.invoke(p);
                                                if (n != null) name = n.toString();
                                                break;
                                            }
                                        }
                                    }
                                    if (name.equalsIgnoreCase(targetName) || name.toLowerCase().contains(targetName.toLowerCase())) {
                                        double x = 0, y = 0, z = 0;
                                        float yaw = 0, pitch = 0, health = 20.0f;
                                        for (Method m : p.getClass().getMethods()) {
                                            if (m.getParameterCount() == 0) {
                                                String mn = m.getName();
                                                if (m.getReturnType() == double.class) {
                                                    double v = (double) m.invoke(p);
                                                    if (mn.equals("getX") || mn.equals("method_23317")) x = v;
                                                    else if (mn.equals("getY") || mn.equals("method_23318")) y = v;
                                                    else if (mn.equals("getZ") || mn.equals("method_23321")) z = v;
                                                }
                                                if (m.getReturnType() == float.class) {
                                                    float v = (float) m.invoke(p);
                                                    if (mn.equals("getYaw") || mn.equals("method_36454")) yaw = v;
                                                    else if (mn.equals("getPitch") || mn.equals("method_36455")) pitch = v;
                                                    else if (mn.equals("getHealth") || mn.equals("method_6032")) health = v;
                                                }
                                            }
                                        }
                                        // Fallback via getPos()
                                        if (x == 0 && y == 0 && z == 0) {
                                            for (Method m : p.getClass().getMethods()) {
                                                if (m.getParameterCount() == 0 && (m.getName().equals("getPos") || m.getName().equals("method_19538") || m.getReturnType().getName().contains("243") || m.getReturnType().getName().contains("Vec3d"))) {
                                                    Object vec = m.invoke(p);
                                                    if (vec != null) {
                                                        for (Field f : vec.getClass().getFields()) {
                                                            if (f.getName().equals("x") || f.getName().equals("field_1352")) x = f.getDouble(vec);
                                                            if (f.getName().equals("y") || f.getName().equals("field_1351")) y = f.getDouble(vec);
                                                            if (f.getName().equals("z") || f.getName().equals("field_1350")) z = f.getDouble(vec);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        String res = String.format("{\"ok\":true,\"player\":\"%s\",\"x\":%.3f,\"y\":%.3f,\"z\":%.3f,\"yaw\":%.2f,\"pitch\":%.2f,\"health\":%.1f}", name, x, y, z, yaw, pitch, health);
                                        System.out.println("[LivePos] " + res);
                                        java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/mc_live_pos.json"));
                                        pw.println(res);
                                        pw.close();
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return;
            }

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
                            if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == String.class && m.getParameterTypes()[0].isAssignableFrom(commandSource.getClass())) {
                                if (m.getName().equals("executeWithPrefix") || m.getName().equals("method_44252")) {
                                    try {
                                        m.invoke(commandManager, commandSource, cleanCmd);
                                        System.out.println("[CommandRunnerAgent] ✅ Comando executado: /" + cleanCmd);
                                        break;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
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
