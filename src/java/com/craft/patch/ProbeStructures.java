package com.craft.patch;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class ProbeStructures {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Object server = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("Server thread")) {
                    for (Field f : t.getClass().getDeclaredFields()) {
                        if (f.getType().getName().contains("3176") || f.getType().getName().contains("MinecraftServer")) {
                            f.setAccessible(true);
                            server = f.get(t);
                            break;
                        }
                    }
                }
                if (server != null) break;
            }
            if (server == null) return;
            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);
            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object cmdMgr = fServer.getClass().getMethod("method_3734").invoke(fServer);
                    Object cmdSrc = fServer.getClass().getMethod("method_3739").invoke(fServer);

                    ClassLoader cl = fServer.getClass().getClassLoader();
                    Class<?> cmdOutputClass = Class.forName("net.minecraft.class_2165", true, cl);

                    List<String> results = new ArrayList<>();

                    Object proxyOutput = Proxy.newProxyInstance(
                        cl,
                        new Class<?>[]{cmdOutputClass},
                        (proxy, method, margs) -> {
                            if (margs != null && margs.length > 0 && margs[0] != null) {
                                try {
                                    Method getStringMethod = margs[0].getClass().getMethod("getString");
                                    results.add((String) getStringMethod.invoke(margs[0]));
                                } catch (Throwable ignored) {
                                    results.add(margs[0].toString());
                                }
                            }
                            return null;
                        }
                    );

                    Object capturingSrc = cmdSrc.getClass().getMethod("method_9220", cmdOutputClass).invoke(cmdSrc, proxyOutput);

                    String[] structs = {
                        "execute at Pace run locate structure minecraft:trial_chambers",
                        "execute at Pace run locate structure minecraft:village_savanna",
                        "execute at Pace run locate structure minecraft:village_plains",
                        "execute at Pace run locate structure minecraft:village_desert",
                        "execute at Pace run locate structure minecraft:ancient_city",
                        "execute at Pace run locate structure minecraft:pillager_outpost",
                        "execute at Pace run locate structure minecraft:ruined_portal",
                        "execute at Pace run locate structure minecraft:shipwreck",
                        "execute at Pace run locate structure minecraft:mineshaft",
                        "execute at Pace run locate structure minecraft:monument",
                        "execute at Pace run locate structure minecraft:mansion"
                    };

                    for (String cmd : structs) {
                        try {
                            cmdMgr.getClass().getMethod("method_44252", cmdSrc.getClass(), String.class)
                                .invoke(cmdMgr, capturingSrc, cmd);
                        } catch (Throwable e) {}
                    }

                    File out = new File("/home/pace/dev/craft-repl/target/nearby_structures.txt");
                    PrintWriter pw = new PrintWriter(new FileWriter(out));
                    for (String s : results) pw.println(s);
                    pw.close();
                    System.out.println("[ProbeStructures] ✅ Encontradas " + results.size() + " estruturas salvas em " + out.getAbsolutePath());

                } catch (Throwable t) { t.printStackTrace(); }
            });
        } catch (Throwable t) { t.printStackTrace(); }
    }
}
