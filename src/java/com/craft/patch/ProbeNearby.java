package com.craft.patch;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ProbeNearby {
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

            if (server == null) {
                System.err.println("[ProbeNearby] Servidor não encontrado.");
                return;
            }

            final Object fServer = server;
            Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);

            executeMethod.invoke(fServer, (Runnable) () -> {
                try {
                    Object pm = fServer.getClass().getMethod("method_3760").invoke(fServer);
                    Object pace = pm.getClass().getMethod("method_14566", String.class).invoke(pm, "Pace");
                    if (pace == null) {
                        System.err.println("[ProbeNearby] Pace offline");
                        return;
                    }

                    Object world = pace.getClass().getMethod("method_51469").invoke(pace);
                    String dim = world.getClass().getMethod("method_27983").invoke(world).toString(); // getRegistryKey

                    double px = (double) pace.getClass().getMethod("method_23317").invoke(pace);
                    double py = (double) pace.getClass().getMethod("method_23318").invoke(pace);
                    double pz = (double) pace.getClass().getMethod("method_23321").invoke(pace);

                    Object blockPos = pace.getClass().getMethod("method_24515").invoke(pace); // getBlockPos
                    Object biomeEntry = world.getClass().getMethod("method_23753", blockPos.getClass()).invoke(world, blockPos); // getBiome
                    String biomeStr = biomeEntry.toString();

                    // Entities in radius 100
                    Map<String, Integer> entityCounts = new TreeMap<>();
                    List<String> specialEntities = new ArrayList<>();

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

                            if (d <= 150.0) {
                                Object typeObj = ent.getClass().getMethod("method_5864").invoke(ent); // getType
                                String typeName = typeObj.toString();
                                entityCounts.put(typeName, entityCounts.getOrDefault(typeName, 0) + 1);

                                if (d <= 60.0 && (typeName.contains("villager") || typeName.contains("golem") || typeName.contains("ship") || typeName.contains("allay") || typeName.contains("wandering") || typeName.contains("breeze") || typeName.contains("bogged"))) {
                                    specialEntities.add(String.format("%s a %.1fm (X: %.0f, Y: %.0f, Z: %.0f)", typeName, d, ex, ey, ez));
                                }
                            }
                        }
                    }

                    // Interesting blocks scan (radius 32, Y between -60 and 120)
                    Class<?> bpClass = blockPos.getClass();
                    java.lang.reflect.Constructor<?> posCtor = bpClass.getConstructor(int.class, int.class, int.class);
                    Method getBlockStateMethod = world.getClass().getMethod("method_8320", bpClass); // getBlockState

                    Map<String, List<String>> pointsOfInterest = new LinkedHashMap<>();
                    int ipx = (int) Math.round(px);
                    int ipz = (int) Math.round(pz);

                    for (int dx = -40; dx <= 40; dx += 2) {
                        for (int dz = -40; dz <= 40; dz += 2) {
                            for (int y = -50; y <= 110; y += 2) {
                                Object p = posCtor.newInstance(ipx + dx, y, ipz + dz);
                                Object st = getBlockStateMethod.invoke(world, p);
                                String s = st.toString();

                                if (s.contains("spawner") || s.contains("chest") || s.contains("diamond_ore") || s.contains("ancient_debris") || s.contains("amethyst_cluster") || s.contains("budding_amethyst") || s.contains("trial_spawner") || s.contains("vault") || s.contains("sculk_catalyst") || s.contains("nether_portal")) {
                                    String key = s.split("\\[|\\{")[0].replace("Block{", "").replace("}", "");
                                    if (!pointsOfInterest.containsKey(key)) {
                                        pointsOfInterest.put(key, new ArrayList<>());
                                    }
                                    if (pointsOfInterest.get(key).size() < 4) {
                                        pointsOfInterest.get(key).add(String.format("[%d, %d, %d]", ipx + dx, y, ipz + dz));
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
                    sb.append("  \"entities_150m\": {\n");
                    int i = 0;
                    for (Map.Entry<String, Integer> e : entityCounts.entrySet()) {
                        sb.append(String.format("    \"%s\": %d%s\n", e.getKey(), e.getValue(), (++i < entityCounts.size() ? "," : "")));
                    }
                    sb.append("  },\n");
                    sb.append("  \"special_entities\": [\n");
                    for (int j = 0; j < specialEntities.size(); j++) {
                        sb.append(String.format("    \"%s\"%s\n", specialEntities.get(j), (j + 1 < specialEntities.size() ? "," : "")));
                    }
                    sb.append("  ],\n");
                    sb.append("  \"points_of_interest\": {\n");
                    int k = 0;
                    for (Map.Entry<String, List<String>> e : pointsOfInterest.entrySet()) {
                        sb.append(String.format("    \"%s\": %s%s\n", e.getKey(), e.getValue().toString(), (++k < pointsOfInterest.size() ? "," : "")));
                    }
                    sb.append("  }\n");
                    sb.append("}\n");

                    File outFile = new File("/home/pace/dev/minecraft/craft-repl/target/nearby_scan.json");
                    PrintWriter pw = new PrintWriter(new FileWriter(outFile));
                    pw.print(sb.toString());
                    pw.close();
                    System.out.println("[ProbeNearby] ✅ Sondagem concluída salva em " + outFile.getAbsolutePath());

                } catch (Throwable t) {
                    System.err.println("[ProbeNearby] Erro no scan:");
                    t.printStackTrace();
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
