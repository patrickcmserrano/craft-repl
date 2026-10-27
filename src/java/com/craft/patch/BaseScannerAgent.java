package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;

public class BaseScannerAgent {
    public static void agentmain(String args, Instrumentation inst) {
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
            if (server == null) return;

            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object playerManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                            playerManager = m.invoke(fServer);
                            if (playerManager != null) break;
                        }
                    }

                    Object host = null;
                    if (playerManager != null) {
                        List<?> players = (List<?>) playerManager.getClass().getMethod("method_14571").invoke(playerManager);
                        if (players == null) {
                            for (Method m : playerManager.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                                    players = (List<?>) m.invoke(playerManager);
                                    if (players != null) break;
                                }
                            }
                        }
                        if (players != null) {
                            for (Object p : players) {
                                if (p.toString().toLowerCase().contains("pace")) host = p;
                            }
                        }
                    }

                    if (host == null) return;

                    Object world = null;
                    for (Method m : host.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getWorld") || m.getName().equals("method_37908"))) {
                            world = m.invoke(host);
                            break;
                        }
                    }

                    int px = -158;
                    int py = 114;
                    int pz = -92;

                    Class<?> blockPosClass = Class.forName("net.minecraft.class_2338", true, world.getClass().getClassLoader());
                    java.lang.reflect.Constructor<?> posCtor = blockPosClass.getConstructor(int.class, int.class, int.class);

                    Method getBlockStateMethod = null;
                    for (Method m : world.getClass().getMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == blockPosClass) {
                            if (m.getName().equals("getBlockState") || m.getName().equals("method_8320")) {
                                getBlockStateMethod = m;
                                break;
                            }
                        }
                    }

                    Map<String, Integer> counts = new HashMap<>();
                    for (int dx = -10; dx <= 10; dx++) {
                        for (int dz = -10; dz <= 10; dz++) {
                            for (int dy = -3; dy <= 3; dy++) {
                                Object pos = posCtor.newInstance(px + dx, py + dy, pz + dz);
                                Object state = getBlockStateMethod.invoke(world, pos);
                                String s = state.toString().split("\\[|\\{")[0].replace("Block", "").replace("{", "").replace("}", "");
                                counts.put(s, counts.getOrDefault(s, 0) + 1);
                            }
                        }
                    }

                    System.out.println("[BaseScanner] 🔍 Blocos detectados em volta da base: " + counts);

                } catch (Exception e) {
                    System.err.println("[BaseScanner] Erro no scan:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
