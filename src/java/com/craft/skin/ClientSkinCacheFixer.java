package com.craft.skin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Map;

public class ClientSkinCacheFixer {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            String[][] mappings = new String[][] {
                {"Lizzie", "6d3fbe2dc6f5354fb12366dec0af223c", "lizzie"},
                {"Picard", "3db477e45e873d89a8aa2f2c20c910ff", "picard"},
                {"Karlach", "7b8f0c8a2917332ab7743457dc13fede", "karlach"},
                {"pace", "bfb04b4641a13e85b0fbbc18eccdb3c6", "pace"}
            };

            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("com.sklauncher.injector.filters.ProfileFilter") ||
                    c.getName().equals("com.sklauncher.injector.filters.ProfileExtendedFilter")) {
                    for (Field f : c.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            Map<String, String> cache = (Map<String, String>) f.get(null);
                            for (String[] m : mappings) {
                                String resp = buildResponse(m[0], m[1], m[2]);
                                if (resp != null) {
                                    cache.put(m[1] + ":true", resp);
                                    cache.put(m[1] + ":false", resp);
                                    cache.put(m[1], resp);
                                }
                            }
                            System.out.println("[ClientSkinCacheFixer] ✅ Cache do SKlauncher atualizado em " + c.getName());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String buildResponse(String playerName, String uuidNoDashes, String skinName) {
        try {
            File metaFile = new File("/home/pace/minecraft-server/skins/" + skinName + "_meta.json");
            if (!metaFile.exists()) {
                metaFile = new File("/home/pace/minecraft-server/skins/" + skinName + ".json");
            }
            if (!metaFile.exists()) return null;

            BufferedReader br = new BufferedReader(new FileReader(metaFile));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
            br.close();
            String json = sb.toString();

            int valStart = json.indexOf("\"value\": \"") + 10;
            int valEnd = json.indexOf("\"", valStart);
            if (valStart < 10 || valEnd < 0) return null;
            String value = json.substring(valStart, valEnd);

            int sigStart = json.indexOf("\"signature\": \"") + 14;
            int sigEnd = json.indexOf("\"", sigStart);
            String sig = (sigStart > 13 && sigEnd > sigStart) ? json.substring(sigStart, sigEnd) : "";

            return "{\"id\":\"" + uuidNoDashes + "\",\"name\":\"" + playerName +
                   "\",\"properties\":[{\"name\":\"textures\",\"value\":\"" + value +
                   "\",\"signature\":\"" + sig + "\"}]}";
        } catch (Exception e) {
            return null;
        }
    }
}
