package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class RepairAgentV2 {
    public static void agentmain(String targetPlayer, Instrumentation inst) {
        System.out.println("[RepairAgentV2] 🔨 Iniciando reparo de equipamentos...");
        try {
            Object server = null;
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

            if (server == null) {
                System.err.println("[RepairAgentV2] ❌ Servidor não encontrado.");
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
            if (executeMethod == null) return;

            final String targetName = (targetPlayer != null && !targetPlayer.trim().isEmpty()) ? targetPlayer.trim() : "pace";

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object pm = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                            pm = m.invoke(fServer);
                            break;
                        }
                    }
                    if (pm == null) return;

                    List<?> players = null;
                    for (Method m : pm.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                            players = (List<?>) m.invoke(pm);
                            break;
                        }
                    }
                    if (players == null) return;

                    int repaired = 0;
                    for (Object p : players) {
                        String pName = "";
                        for (Method m : p.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getName") || m.getName().equals("getEntityName") || m.getReturnType().getName().contains("Text") || m.getReturnType().getName().contains("class_2561"))) {
                                Object res = m.invoke(p);
                                if (res != null) {
                                    for (Method sm : res.getClass().getMethods()) {
                                        if (sm.getName().equals("getString") && sm.getParameterCount() == 0) {
                                            pName = (String) sm.invoke(res);
                                            break;
                                        }
                                    }
                                }
                                if (!pName.isEmpty()) break;
                            }
                        }

                        if (!targetName.equalsIgnoreCase("all") && !targetName.equalsIgnoreCase("@a") && !pName.equalsIgnoreCase(targetName)) {
                            continue;
                        }

                        Object inv = null;
                        for (Method m : p.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getInventory") || m.getReturnType().getName().contains("1661"))) {
                                inv = m.invoke(p);
                                break;
                            }
                        }
                        if (inv == null) continue;

                        for (Field f : inv.getClass().getDeclaredFields()) {
                            if (List.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                List<?> list = (List<?>) f.get(inv);
                                if (list != null) {
                                    for (Object item : list) {
                                        if (item == null) continue;
                                        // setDamage(0)
                                        for (Method im : item.getClass().getMethods()) {
                                            if (im.getParameterCount() == 1 && im.getParameterTypes()[0] == int.class &&
                                                (im.getName().equals("setDamage") || im.getName().equals("method_7956"))) {
                                                im.invoke(item, 0);
                                                repaired++;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        System.out.println("[RepairAgentV2] ✅ Equipamentos e inventário de " + pName + " reparados para 100% de durabilidade! (" + repaired + " slots processados)");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
