package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class LivePosAgent {
    public static void agentmain(String args, Instrumentation inst) {
        String targetPlayer = (args != null && !args.trim().isEmpty()) ? args.trim() : "pace";
        
        try {
            Object server = null;

            // 1. Localizar servidor via threads ativas
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("Server thread") || t.getName().contains("VoiceChatServerThread")) {
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

            // Fallback: FabricLoader
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
                            if (m.getParameterCount() == 0 && (m.getName().equals("getServer") || m.getReturnType().getName().contains("1132") || m.getReturnType().getName().contains("Server"))) {
                                server = m.invoke(gameInstance);
                                if (server != null) break;
                            }
                        }
                    }
                }
            }

            if (server == null) {
                System.err.println("{\"ok\":false,\"error\":\"Server not found\"}");
                return;
            }

            // 2. Obter PlayerManager
            Object playerManager = null;
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324") || m.getReturnType().getName().contains("PlayerManager"))) {
                    playerManager = m.invoke(server);
                    if (playerManager != null) break;
                }
            }

            if (playerManager == null) {
                System.err.println("{\"ok\":false,\"error\":\"PlayerManager not found\"}");
                return;
            }

            // 3. Obter lista de jogadores online
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

            if (players == null || players.isEmpty()) {
                System.err.println("{\"ok\":false,\"error\":\"No players online\"}");
                return;
            }

            Object target = null;
            String foundName = "";

            for (Object p : players) {
                String pName = "";
                // Get profile or name
                for (Method m : p.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("method_5477") || m.getName().equals("method_5820"))) {
                        Object n = m.invoke(p);
                        if (n != null) pName = n.toString();
                        break;
                    }
                }
                // Try getGameProfile
                if (pName.isEmpty()) {
                    for (Method m : p.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getGameProfile") || m.getReturnType().getName().contains("GameProfile"))) {
                            Object prof = m.invoke(p);
                            if (prof != null) {
                                pName = (String) prof.getClass().getMethod("getName").invoke(prof);
                                break;
                            }
                        }
                    }
                }

                if (pName.equalsIgnoreCase(targetPlayer) || pName.toLowerCase().contains(targetPlayer.toLowerCase())) {
                    target = p;
                    foundName = pName;
                    break;
                }
            }

            if (target == null) {
                System.err.println("{\"ok\":false,\"error\":\"Player " + targetPlayer + " not found\"}");
                return;
            }

            // Write debug to file
            PrintWriter debugOut = new PrintWriter(new FileWriter("/tmp/mc_live_debug.txt"));
            debugOut.println("Target class: " + target.getClass().getName());
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    debugOut.println("Method: " + m.getName() + " -> " + m.getReturnType().getName());
                }
            }
            debugOut.close();

            // 4. Extrair coordenadas, rotação e status ao vivo
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0, health = 20.0f;
            String dimension = "minecraft:overworld";

            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    String mName = m.getName();
                    if (m.getReturnType() == double.class) {
                        try {
                            double val = (double) m.invoke(target);
                            System.out.println("[LivePosDebug] double " + mName + " = " + val);
                            if (mName.equals("getX") || mName.equals("method_23317")) x = val;
                            if (mName.equals("getY") || mName.equals("method_23318")) y = val;
                            if (mName.equals("getZ") || mName.equals("method_23321")) z = val;
                        } catch (Exception ignored) {}
                    }
                    if (m.getReturnType() == float.class) {
                        try {
                            float val = (float) m.invoke(target);
                            System.out.println("[LivePosDebug] float " + mName + " = " + val);
                            if (mName.equals("getYaw") || mName.equals("method_36454")) yaw = val;
                            if (mName.equals("getPitch") || mName.equals("method_36455")) pitch = val;
                            if (mName.equals("getHealth") || mName.equals("method_6032")) health = val;
                        } catch (Exception ignored) {}
                    }
                    if (mName.equals("getWorld") || mName.equals("method_37908")) {
                        try {
                            Object world = m.invoke(target);
                            if (world != null) {
                                for (Method wm : world.getClass().getMethods()) {
                                    if (wm.getName().equals("getRegistryKey") || wm.getName().equals("method_27983")) {
                                        Object regKey = wm.invoke(world);
                                        dimension = regKey.toString();
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            String jsonResult = String.format(
                "{\"ok\":true,\"player\":\"%s\",\"x\":%.3f,\"y\":%.3f,\"z\":%.3f,\"yaw\":%.2f,\"pitch\":%.2f,\"health\":%.1f,\"dimension\":\"%s\"}",
                foundName, x, y, z, yaw, pitch, health, dimension
            );

            // Print on stdout for caller
            System.out.println("[LivePos] " + jsonResult);

            // Write to shared cache file /tmp/mc_live_pos.json
            try {
                PrintWriter pw = new PrintWriter(new FileWriter("/tmp/mc_live_pos.json"));
                pw.println(jsonResult);
                pw.close();
            } catch (Exception ignored) {}

        } catch (Throwable t) {
            System.err.println("{\"ok\":false,\"error\":\"" + t.getMessage() + "\"}");
            t.printStackTrace();
        }
    }
}
