package com.craft.skin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class SkinRestorer {
    public static void agentmain(String args, Instrumentation inst) {
        String targetPlayer = (args != null && !args.trim().isEmpty()) ? args.trim().split(" ")[0] : "pace";
        String skinName = (args != null && args.trim().split(" ").length > 1) ? args.trim().split(" ")[1] : "pace";
        System.out.println("[SkinRestorer] Aplicando skin offline '" + skinName + "' para jogador: " + targetPlayer);

        try {
            File metaFile = new File("/home/pace/minecraft-server/skins/" + skinName.toLowerCase() + "_meta.json");
            if (!metaFile.exists()) {
                metaFile = new File("/home/pace/minecraft-server/skins/" + skinName.toLowerCase() + ".json");
            }
            if (!metaFile.exists()) {
                System.err.println("[SkinRestorer] Arquivo não encontrado: " + metaFile.getAbsolutePath());
                return;
            }

            BufferedReader br = new BufferedReader(new FileReader(metaFile));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
            br.close();
            String json = sb.toString();

            int valStart = json.indexOf("\"value\": \"") + 10;
            int valEnd = json.indexOf("\"", valStart);
            final String value = json.substring(valStart, valEnd);

            int sigStart = json.indexOf("\"signature\": \"") + 14;
            int sigEnd = json.indexOf("\"", sigStart);
            final String sig = (sigStart > 13 && sigEnd > sigStart) ? json.substring(sigStart, sigEnd) : "";

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
                System.err.println("[SkinRestorer] Servidor não encontrado.");
                return;
            }

            Object playerManager = null;
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                    playerManager = m.invoke(server);
                    if (playerManager != null) break;
                }
            }

            List<?> players = null;
            if (playerManager != null) {
                for (Method m : playerManager.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                        players = (List<?>) m.invoke(playerManager);
                        if (players != null) break;
                    }
                }
            }

            if (players == null) return;
            for (final Object p : players) {
                Method getProfile = null;
                for (Method pm : p.getClass().getMethods()) {
                    if (pm.getParameterCount() == 0 && (pm.getName().equals("getGameProfile") || pm.getReturnType().getName().contains("GameProfile"))) {
                        getProfile = pm;
                        break;
                    }
                }
                if (getProfile == null) continue;
                Object profile = getProfile.invoke(p);
                String pName = (String) profile.getClass().getMethod("getName").invoke(profile);
                if (pName.equalsIgnoreCase(targetPlayer)) {
                    System.out.println("[SkinRestorer] Jogador alvo encontrado no servidor: " + pName);

                    Method setSkinMethod = null;
                    for (Method sm : p.getClass().getMethods()) {
                        if (sm.getName().contains("fabrictailor_setSkin") && sm.getParameterCount() == 3) {
                            Class<?>[] pTypes = sm.getParameterTypes();
                            if (pTypes[0] == String.class && pTypes[1] == String.class) {
                                setSkinMethod = sm;
                                break;
                            }
                        }
                    }

                    if (setSkinMethod != null) {
                        final Method fSetSkin = setSkinMethod;
                        final Object fServer = server;
                        Method execMethod = null;
                        for (Method m : fServer.getClass().getMethods()) {
                            if (m.getName().equals("execute") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                                execMethod = m;
                                break;
                            }
                        }

                        if (execMethod != null) {
                            execMethod.invoke(fServer, (Runnable) () -> {
                                try {
                                    fSetSkin.invoke(p, value, sig, Boolean.TRUE);
                                    System.out.println("[SkinRestorer] ✅ fabrictailor_setSkin executado no Main Server Thread!");
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                }
                            });
                        } else {
                            fSetSkin.invoke(p, value, sig, Boolean.TRUE);
                        }
                        System.out.println("[SkinRestorer] 🎉 Skin restaurada com sucesso para " + pName + "!");
                    } else {
                        System.err.println("[SkinRestorer] Método fabrictailor_setSkin não encontrado.");
                    }
                    break;
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
