package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ProbeAgent {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Object server = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("Server thread")) {
                    for (Field f : t.getClass().getDeclaredFields()) {
                        if (f.getType().getName().contains("DedicatedServer") || f.getType().getName().contains("3176")) {
                            f.setAccessible(true);
                            server = f.get(t);
                            if (server != null) break;
                        }
                    }
                }
                if (server != null) break;
            }
            if (server == null) return;

            Object pm = null;
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                    pm = m.invoke(server);
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
            if (players == null || players.isEmpty()) return;

            Object player = players.get(0);
            Object inv = null;
            for (Method m : player.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getInventory") || m.getReturnType().getName().contains("1661"))) {
                    inv = m.invoke(player);
                    break;
                }
            }
            System.out.println("[Probe] Player inventory: " + inv);
            if (inv != null) {
                for (Field f : inv.getClass().getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        List<?> list = (List<?>) f.get(inv);
                        System.out.println("[Probe] Inv list field: " + f.getName() + " size: " + (list != null ? list.size() : -1));
                        if (list != null && !list.isEmpty()) {
                            for (Object item : list) {
                                if (item != null) {
                                    System.out.println("[Probe] Item class: " + item.getClass().getName());
                                    for (Method im : item.getClass().getMethods()) {
                                        if (im.getName().toLowerCase().contains("damage")) {
                                            System.out.println("[Probe] Damage method: " + im);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
