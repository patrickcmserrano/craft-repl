
package com.craft.patch;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;

public class ProbeMethods {
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
                try {
                    Object pm = fServer.getClass().getMethod("getPlayerManager").invoke(fServer);
                    List<?> players = (List<?>) pm.getClass().getMethod("getPlayerList").invoke(pm);
                    Object p = players.get(0);
                    Object inv = p.getClass().getMethod("getInventory").invoke(p);
                    System.out.println("[ProbeMethods] inv class: " + inv.getClass().getName());
                    for (Method m : inv.getClass().getMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class) {
                            System.out.println("[ProbeMethods] inv m(int): " + m.getName() + " returns " + m.getReturnType().getSimpleName());
                        }
                    }
                    Object mainHand = p.getClass().getMethod("getMainHandStack").invoke(p);
                    System.out.println("[ProbeMethods] mainHand: " + mainHand);
                    if (mainHand != null) {
                        for (Method m : mainHand.getClass().getMethods()) {
                            if (m.getName().toLowerCase().contains("damag")) {
                                System.out.println("[ProbeMethods] stack m: " + m);
                            }
                        }
                    }
                } catch(Throwable t) { t.printStackTrace(); }
            });
        } catch(Throwable t) {}
    }
}
