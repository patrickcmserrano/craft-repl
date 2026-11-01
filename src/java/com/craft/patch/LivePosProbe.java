package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * LivePosProbe - Dynamic Java Agent for instant real-time player telemetry.
 * Queries coordinates, look angles (yaw/pitch), health, and dimension directly from the JVM.
 */
public class LivePosProbe {

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

            if (server == null) {
                writeResult("{\"ok\":false,\"error\":\"Server thread or instance not found\"}");
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
                writeResult("{\"ok\":false,\"error\":\"PlayerManager not found\"}");
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
                writeResult("{\"ok\":false,\"error\":\"No players online\"}");
                return;
            }

            Object target = null;
            String foundName = "";

            for (Object p : players) {
                String pName = "";
                for (Method m : p.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && (m.getName().equals("getGameProfile") || m.getReturnType().getName().contains("GameProfile"))) {
                        try {
                            Object prof = m.invoke(p);
                            if (prof != null) {
                                pName = (String) prof.getClass().getMethod("getName").invoke(prof);
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (pName.isEmpty()) {
                    for (Method m : p.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("method_5477"))) {
                            try {
                                Object n = m.invoke(p);
                                if (n != null) pName = n.toString();
                                break;
                            } catch (Exception ignored) {}
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
                writeResult(String.format("{\"ok\":false,\"error\":\"Player '%s' not found online\"}", targetPlayer));
                return;
            }

            // 4. Extrair telemetria ao vivo
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0, health = 20.0f;
            String dimension = "minecraft:overworld";

            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    String mName = m.getName();
                    if (m.getReturnType() == double.class) {
                        try {
                            double val = (double) m.invoke(target);
                            if (mName.equals("getX") || mName.equals("method_23317")) x = val;
                            else if (mName.equals("getY") || mName.equals("method_23318")) y = val;
                            else if (mName.equals("getZ") || mName.equals("method_23321")) z = val;
                        } catch (Exception ignored) {}
                    }
                    if (m.getReturnType() == float.class) {
                        try {
                            float val = (float) m.invoke(target);
                            if (mName.equals("getYaw") || mName.equals("method_36454")) yaw = val;
                            else if (mName.equals("getPitch") || mName.equals("method_36455")) pitch = val;
                            else if (mName.equals("getHealth") || mName.equals("method_6032")) health = val;
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Fallback Vec3d
            if (x == 0 && y == 0 && z == 0) {
                for (Method m : target.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && (m.getName().equals("getPos") || m.getName().equals("method_19538") || m.getReturnType().getName().contains("243") || m.getReturnType().getName().contains("Vec3d"))) {
                        try {
                            Object vec = m.invoke(target);
                            if (vec != null) {
                                for (Field f : vec.getClass().getFields()) {
                                    if (f.getName().equals("x") || f.getName().equals("field_1352")) x = f.getDouble(vec);
                                    if (f.getName().equals("y") || f.getName().equals("field_1351")) y = f.getDouble(vec);
                                    if (f.getName().equals("z") || f.getName().equals("field_1350")) z = f.getDouble(vec);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Dimensão
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getWorld") || m.getName().equals("method_37908") || m.getReturnType().getName().contains("ServerWorld") || m.getReturnType().getName().contains("3218"))) {
                    try {
                        Object world = m.invoke(target);
                        if (world != null) {
                            for (Method wm : world.getClass().getMethods()) {
                                if (wm.getParameterCount() == 0 && (wm.getName().equals("getRegistryKey") || wm.getName().equals("method_27983"))) {
                                    Object regKey = wm.invoke(world);
                                    if (regKey != null) {
                                        for (Method rkm : regKey.getClass().getMethods()) {
                                            if (rkm.getParameterCount() == 0 && (rkm.getName().equals("getValue") || rkm.getName().equals("method_29177"))) {
                                                Object val = rkm.invoke(regKey);
                                                if (val != null) dimension = val.toString();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            String json = String.format(
                "{\"ok\":true,\"player\":\"%s\",\"x\":%.3f,\"y\":%.3f,\"z\":%.3f,\"yaw\":%.2f,\"pitch\":%.2f,\"health\":%.1f,\"dimension\":\"%s\"}",
                foundName, x, y, z, yaw, pitch, health, dimension
            );

            writeResult(json);
            System.out.println("[LivePosProbe] " + json);

        } catch (Throwable t) {
            String errJson = String.format("{\"ok\":false,\"error\":\"%s\"}", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            writeResult(errJson);
            t.printStackTrace();
        }
    }

    private static void writeResult(String json) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/mc_live_pos.json"))) {
            pw.println(json);
        } catch (Exception ignored) {}
    }
}
