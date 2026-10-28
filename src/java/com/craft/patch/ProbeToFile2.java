
package com.craft.patch;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

public class ProbeToFile2 {
    public static void agentmain(String args, Instrumentation inst) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
            pw.println("=== ProbeToFile2 Started ===");
            Object server = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("VoiceChatServerThread") || t.getName().contains("Server thread")) {
                    for (Field f : t.getClass().getDeclaredFields()) {
                        String typeName = f.getType().getName();
                        if (typeName.contains("3176") || typeName.contains("1132")) {
                            f.setAccessible(true);
                            server = f.get(t);
                            if (server != null) break;
                        }
                    }
                }
                if (server != null) break;
            }
            pw.println("server: " + server);
            if (server == null) return;
            final Object fServer = server;
            Method exec = fServer.getClass().getMethod("execute", Runnable.class);
            exec.invoke(fServer, (Runnable) () -> {
                try (PrintWriter pw2 = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
                    pw2.println("Inside server execute!");
                    Object pm = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324") || m.getReturnType().getName().contains("3174"))) {
                            pm = m.invoke(fServer);
                            break;
                        }
                    }
                    pw2.println("pm: " + pm);
                    if (pm == null) return;
                    List<?> players = null;
                    for (Method m : pm.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                            pw2.println("Found list method: " + m.getName());
                            players = (List<?>) m.invoke(pm);
                            if (players != null) break;
                        }
                    }
                    pw2.println("players size: " + (players != null ? players.size() : -1));
                    if (players != null && !players.isEmpty()) {
                        Object p = players.get(0);
                        pw2.println("player: " + p);
                        Object inv = null;
                        for (Method m : p.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && (m.getName().equals("getInventory") || m.getReturnType().getName().contains("1661"))) {
                                inv = m.invoke(p);
                                break;
                            }
                        }
                        pw2.println("inv: " + inv);
                        if (inv != null) {
                            for (Field f : inv.getClass().getDeclaredFields()) {
                                f.setAccessible(true);
                                Object v = f.get(inv);
                                pw2.println("field " + f.getName() + " (" + f.getType().getSimpleName() + ") -> " + (v instanceof List ? ((List<?>)v).size() : v));
                                if (v instanceof List) {
                                    List<?> l = (List<?>) v;
                                    for (Object it : l) {
                                        if (it != null) {
                                            for (Method im : it.getClass().getMethods()) {
                                                if (im.getName().toLowerCase().contains("damag")) {
                                                    pw2.println("item damage m: " + im);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch(Throwable t) {
                    try (PrintWriter pw3 = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
                        t.printStackTrace(pw3);
                    } catch(Exception e) {}
                }
            });
        } catch(Throwable t) {
            try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
                t.printStackTrace(pw);
            } catch(Exception e) {}
        }
    }
}
