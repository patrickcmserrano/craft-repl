package com.craft.patch;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ProbeNearbyV3 {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Object server = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().contains("Server thread") || t.getName().contains("VoiceChatServerThread")) {
                    for (Field f : t.getClass().getDeclaredFields()) {
                        String typeName = f.getType().getName();
                        if (typeName.contains("3176") || typeName.contains("MinecraftServer") || typeName.contains("DedicatedServer")) {
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

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object pm = fServer.getClass().getMethod("method_3760").invoke(fServer);
                    Object pace = pm.getClass().getMethod("method_14566", String.class).invoke(pm, "Pace");
                    if (pace == null) return;

                    Object world = pace.getClass().getMethod("method_51469").invoke(pace);
                    String dim = world.getClass().getMethod("method_27983").invoke(world).toString();

                    double px = (double) pace.getClass().getMethod("method_23317").invoke(pace);
                    double py = (double) pace.getClass().getMethod("method_23318").invoke(pace);
                    double pz = (double) pace.getClass().getMethod("method_23321").invoke(pace);

                    Object blockPos = pace.getClass().getMethod("method_24515").invoke(pace);
                    Object biomeEntry = world.getClass().getMethod("method_23753", blockPos.getClass()).invoke(world, blockPos);
                    String biomeStr = biomeEntry.toString();

                    // Entities in radius 200m
                    Map<String, Integer> entityCounts = new TreeMap<>();
                    List<String> nearbyHighlights = new ArrayList<>();

                    Method iterateEntities = null;
                    for (Method m : world.getClass().getMethods()) {
                        if (m.getName().equals("method_18456") || m.getName().equals("iterateEntities")) {
                            iterateEntities = m;
                            break;
                        }
                    }

                    if (iterateEntities != null) {
                        Iterable<?> allEntities = (Iterable<?>) iterateEntities.invoke(world);
                        for (Object ent : allEntities) {
                            if (ent == null) continue;
                            double ex = (double) ent.getClass().getMethod("method_23317").invoke(ent);
                            double ey = (double) ent.getClass().getMethod("method_23318").invoke(ent);
                            double ez = (double) ent.getClass().getMethod("method_23321").invoke(ent);
                            double d = Math.sqrt((ex - px)*(ex - px) + (ey - py)*(ey - py) + (ez - pz)*(ez - pz));

                            if (d <= 200.0) {
                                Object typeObj = ent.getClass().getMethod("method_5864").invoke(ent);
                                String typeName = typeObj.toString().replace("entity.", "");
                                entityCounts.put(typeName, entityCounts.getOrDefault(typeName, 0) + 1);

                                if (d <= 80.0) {
                                    nearbyHighlights.add(String.format("%s a %.0fm (X: %.0f, Y: %.0f, Z: %.0f)", typeName, d, ex, ey, ez));
                                }
                            }
                        }
                    }

                    // Blocks scan
                    Class<?> bpClass = blockPos.getClass();
                    java.lang.reflect.Constructor<?> posCtor = bpClass.getConstructor(int.class, int.class, int.class);
                    Method getBlockStateMethod = world.getClass().getMethod("method_8320", bpClass);

                    Map<String, List<String>> pointsOfInterest = new LinkedHashMap<>();
                    int ipx = (int) Math.round(px);
                    int ipz = (int) Math.round(pz);

                    // Scan radius 48, from Y=-58 up to Y=120
                    for (int dx = -50; dx <= 50; dx += 2) {
                        for (int dz = -50; dz <= 50; dz += 2) {
                            for (int y = -58; y <= 120; y += 2) {
                                Object p = posCtor.newInstance(ipx + dx, y, ipz + dz);
                                Object st = getBlockStateMethod.invoke(world, p);
                                String s = st.toString();

                                // Extract exact block ID between { and } or [
                                String blockName = "unknown";
                                if (s.contains("{") && s.contains("}")) {
                                    blockName = s.substring(s.indexOf('{') + 1, s.indexOf('}'));
                                }

                                if (blockName.contains("diamond") || blockName.contains("spawner") || 
                                    blockName.contains("chest") || blockName.contains("ancient_debris") || 
                                    blockName.contains("amethyst") || blockName.contains("portal") || 
                                    blockName.contains("gold_ore") || blockName.contains("iron_ore") ||
                                    blockName.contains("copper_ore") || blockName.contains("emerald_ore") ||
                                    blockName.contains("trial") || blockName.contains("vault")) {
                                    
                                    if (!pointsOfInterest.containsKey(blockName)) {
                                        pointsOfInterest.put(blockName, new ArrayList<>());
                                    }
                                    if (pointsOfInterest.get(blockName).size() < 6) {
                                        pointsOfInterest.get(blockName).add(String.format("[%d, %d, %d]", ipx + dx, y, ipz + dz));
                                    }
                                }
                            }
                        }
                    }

                    // Output json
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\n");
                    sb.append(String.format("  \"player\": \"Pace\",\n"));
                    sb.append(String.format("  \"coords\": [%.2f, %.2f, %.2f],\n", px, py, pz));
                    sb.append(String.format("  \"dimension\": \"%s\",\n", dim));
                    sb.append(String.format("  \"biome\": \"%s\",\n", biomeStr));
                    sb.append("  \"entities_nearby\": {\n");
                    int i = 0;
                    for (Map.Entry<String, Integer> e : entityCounts.entrySet()) {
                        sb.append(String.format("    \"%s\": %d%s\n", e.getKey(), e.getValue(), (++i < entityCounts.size() ? "," : "")));
                    }
                    sb.append("  },\n");
                    sb.append("  \"closest_entities\": [\n");
                    for (int j = 0; j < Math.min(15, nearbyHighlights.size()); j++) {
                        sb.append(String.format("    \"%s\"%s\n", nearbyHighlights.get(j), (j + 1 < Math.min(15, nearbyHighlights.size()) ? "," : "")));
                    }
                    sb.append("  ],\n");
                    sb.append("  \"points_of_interest\": {\n");
                    int k = 0;
                    for (Map.Entry<String, List<String>> e : pointsOfInterest.entrySet()) {
                        sb.append(String.format("    \"%s\": %s%s\n", e.getKey(), e.getValue().toString(), (++k < pointsOfInterest.size() ? "," : "")));
                    }
                    sb.append("  }\n");
                    sb.append("}\n");

                    File outFile = new File("/home/pace/dev/craft-repl/target/nearby_scan_v3.json");
                    PrintWriter pw = new PrintWriter(new FileWriter(outFile));
                    pw.print(sb.toString());
                    pw.close();
                    System.out.println("[ProbeNearbyV3] ✅ Sondagem concluída salva em " + outFile.getAbsolutePath());

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
