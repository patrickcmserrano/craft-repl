package com.craft.debug;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class InspectLizzieSkin {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[InspectSkin] Verificando GameProfile de Lizzie...");
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
                System.out.println("[InspectSkin] Servidor não encontrado.");
                return;
            }

            Object playerManager = null;
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("3324"))) {
                    playerManager = m.invoke(server);
                    if (playerManager != null) break;
                }
            }
            if (playerManager == null) {
                System.out.println("[InspectSkin] PlayerManager não encontrado.");
                return;
            }

            List<?> players = null;
            for (Method m : playerManager.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                    players = (List<?>) m.invoke(playerManager);
                    if (players != null) break;
                }
            }
            if (players == null) return;

            for (Object p : players) {
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
                if (pName.equalsIgnoreCase("Lizzie")) {
                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    Method getMethod = properties.getClass().getMethod("get", Object.class);
                    Collection<?> textures = (Collection<?>) getMethod.invoke(properties, "textures");
                    System.out.println("[InspectSkin] Lizzie profile found! Textures count = " + (textures != null ? textures.size() : "null"));
                    if (textures != null) {
                        for (Object prop : textures) {
                            String val = (String) prop.getClass().getMethod("value").invoke(prop);
                            String sig = (String) prop.getClass().getMethod("signature").invoke(prop);
                            System.out.println("[InspectSkin] Prop value length=" + (val != null ? val.length() : "null") + " sig length=" + (sig != null ? sig.length() : "null"));
                            if (val != null) {
                                String decoded = new String(java.util.Base64.getDecoder().decode(val));
                                System.out.println("[InspectSkin] Decoded: " + decoded);
                            }
                        }
                    }
                    // Also check fabrictailor_getSkinValue
                    for (Method sm : p.getClass().getMethods()) {
                        if (sm.getName().equals("fabrictailor_getSkinValue") && sm.getParameterCount() == 0) {
                            Object opt = sm.invoke(p);
                            System.out.println("[InspectSkin] fabrictailor_getSkinValue = " + opt);
                        }
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
