package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SetViewDistanceAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SetViewDistanceAgent] 🔍 Localizando servidor e atualizando view distance...");
        try {
            int viewDistance = 24;
            int simDistance = 12;

            if (args != null && !args.trim().isEmpty()) {
                String[] parts = args.trim().split(" ");
                if (parts.length > 0) viewDistance = Integer.parseInt(parts[0]);
                if (parts.length > 1) simDistance = Integer.parseInt(parts[1]);
            }

            final int targetVd = viewDistance;
            final int targetSd = simDistance;

            Object server = null;
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
                System.err.println("[SetViewDistanceAgent] ❌ Servidor não encontrado em threads ativas.");
                return;
            }

            final Object fServer = server;
            Method executeMethod = null;
            for (Method m : fServer.getClass().getMethods()) {
                if (m.getName().equals("execute") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                    executeMethod = m;
                    break;
                }
            }

            if (executeMethod == null) {
                System.err.println("[SetViewDistanceAgent] ❌ executeMethod não encontrado.");
                return;
            }

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object playerManager = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324") || m.getReturnType().getName().contains("PlayerManager"))) {
                            playerManager = m.invoke(fServer);
                            if (playerManager != null) break;
                        }
                    }

                    if (playerManager == null) {
                        System.err.println("[SetViewDistanceAgent] ❌ PlayerManager não encontrado.");
                        return;
                    }

                    Method setVd = null;
                    Method setSd = null;

                    try {
                        setVd = playerManager.getClass().getMethod("method_14608", int.class);
                    } catch (NoSuchMethodException e) {
                        // ignore
                    }

                    try {
                        setSd = playerManager.getClass().getMethod("method_38650", int.class);
                    } catch (NoSuchMethodException e) {
                        // ignore
                    }

                    if (setVd != null) {
                        setVd.invoke(playerManager, targetVd);
                        System.out.println("[SetViewDistanceAgent] ✅ View distance alterado com sucesso para: " + targetVd);
                    } else {
                        System.err.println("[SetViewDistanceAgent] ❌ Método method_14608 não encontrado!");
                    }

                    if (setSd != null) {
                        setSd.invoke(playerManager, targetSd);
                        System.out.println("[SetViewDistanceAgent] ✅ Simulation distance alterado com sucesso para: " + targetSd);
                    } else {
                        System.err.println("[SetViewDistanceAgent] ❌ Método method_38650 não encontrado!");
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
