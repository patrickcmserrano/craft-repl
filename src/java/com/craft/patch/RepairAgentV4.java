package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class RepairAgentV4 {
    public static void agentmain(String targetPlayer, Instrumentation inst) {
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
            if (server == null) return;

            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);

            final String targetName = (targetPlayer != null && !targetPlayer.trim().isEmpty()) ? targetPlayer.trim() : "pace";

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object pm = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324") || m.getReturnType().getName().contains("3174") || m.getReturnType().getName().contains("PlayerManager"))) {
                            pm = m.invoke(fServer);
                            break;
                        }
                    }
                    if (pm == null) return;

                    List<?> players = null;
                    for (Method m : pm.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                            players = (List<?>) m.invoke(pm);
                            if (players != null && !players.isEmpty()) break;
                        }
                    }
                    if (players == null) return;

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

                        int size = 0;
                        Method getStackMethod = null;
                        for (Method m : inv.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("size") || m.getName().equals("method_5439") || m.getName().equals("getContainerSize"))) {
                                size = (Integer) m.invoke(inv);
                            }
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class &&
                                (m.getName().equals("getStack") || m.getName().equals("method_5438") || m.getName().equals("getItem"))) {
                                getStackMethod = m;
                            }
                        }

                        int repaired = 0;
                        if (getStackMethod != null && size > 0) {
                            for (int i = 0; i < size; i++) {
                                Object item = getStackMethod.invoke(inv, i);
                                if (item == null) continue;
                                Method isEmptyM = null;
                                for (Method m : item.getClass().getMethods()) {
                                    if (m.getParameterCount() == 0 && (m.getName().equals("isEmpty") || m.getName().equals("method_7960"))) {
                                        isEmptyM = m;
                                        break;
                                    }
                                }
                                if (isEmptyM != null && (Boolean) isEmptyM.invoke(item)) continue;

                                int dmg = -1;
                                for (Method m : item.getClass().getMethods()) {
                                    if (m.getParameterCount() == 0 && (m.getName().equals("getDamage") || m.getName().equals("method_7919"))) {
                                        dmg = (Integer) m.invoke(item);
                                        break;
                                    }
                                }

                                if (dmg > 0) {
                                    boolean fixed = false;
                                    for (Method m : item.getClass().getMethods()) {
                                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class &&
                                            (m.getName().equals("setDamage") || m.getName().equals("method_7956"))) {
                                            m.invoke(item, 0);
                                            fixed = true;
                                            break;
                                        }
                                    }
                                    if (fixed) repaired++;
                                }
                            }
                        }
                        System.out.println("[RepairAgentV4] 🎉 Sucesso absoluto! Reparados " + repaired + " itens de " + pName + "!");
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
