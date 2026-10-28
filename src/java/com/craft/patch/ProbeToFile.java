
package com.craft.patch;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

public class ProbeToFile {
    public static void agentmain(String args, Instrumentation inst) {
        try {
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
            if (server == null) return;
            final Object fServer = server;
            Method exec = fServer.getClass().getMethod("execute", Runnable.class);
            exec.invoke(fServer, (Runnable) () -> {
                try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
                    Object pm = fServer.getClass().getMethod("getPlayerManager").invoke(fServer);
                    List<?> players = (List<?>) pm.getClass().getMethod("getPlayerList").invoke(pm);
                    Object p = players.get(0);
                    Object inv = p.getClass().getMethod("getInventory").invoke(p);
                    pw.println("inv class: " + inv.getClass().getName());
                    for (Field f : inv.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        Object val = f.get(inv);
                        pw.println("Field " + f.getName() + " type: " + f.getType().getSimpleName() + " val: " + (val instanceof List ? ((List<?>)val).size() : val));
                    }
                    Object mainHand = p.getClass().getMethod("getMainHandStack").invoke(p);
                    pw.println("mainHand: " + mainHand);
                    if (mainHand != null) {
                        for (Method m : mainHand.getClass().getMethods()) {
                            if (m.getName().toLowerCase().contains("damag")) {
                                pw.println("stack m: " + m);
                            }
                        }
                    }
                } catch(Throwable t) {
                    try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/repair.log", true))) {
                        t.printStackTrace(pw);
                    } catch(Exception e) {}
                }
            });
        } catch(Throwable t) {}
    }
}
