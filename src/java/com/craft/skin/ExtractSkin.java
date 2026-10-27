package com.craft.skin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.io.InputStream;

public class ExtractSkin {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("=== ExtractSkin Running ===");
        try {
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
                System.out.println("Server not found!");
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
                System.out.println("PlayerManager not found!");
                return;
            }

            List<?> players = null;
            for (Method m : playerManager.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                    players = (List<?>) m.invoke(playerManager);
                    if (players != null) break;
                }
            }

            System.out.println("Online players count: " + (players != null ? players.size() : 0));
            if (players != null) {
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
                    System.out.println("Inspecting player: " + pName);

                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    Method getMethod = properties.getClass().getMethod("get", Object.class);
                    Collection<?> textureProps = (Collection<?>) getMethod.invoke(properties, "textures");
                    if (textureProps != null && !textureProps.isEmpty()) {
                        for (Object prop : textureProps) {
                            String value = (String) prop.getClass().getMethod("value").invoke(prop);
                            String sig = null;
                            try { sig = (String) prop.getClass().getMethod("signature").invoke(prop); } catch (Throwable ignored) {}
                            byte[] decoded = Base64.getDecoder().decode(value);
                            String jsonStr = new String(decoded, "UTF-8");
                            System.out.println("  TEXTURE JSON: " + jsonStr);

                            // Extract URL
                            int urlIdx = jsonStr.indexOf("http://textures.minecraft.net/texture/");
                            if (urlIdx == -1) urlIdx = jsonStr.indexOf("https://textures.minecraft.net/texture/");
                            if (urlIdx != -1) {
                                int endIdx = jsonStr.indexOf("\"", urlIdx);
                                String textureUrl = jsonStr.substring(urlIdx, endIdx);
                                System.out.println("  TEXTURE URL: " + textureUrl);

                                // Download PNG directly to ~/minecraft-server/skins/<name>.png
                                File outDir = new File("/home/pace/minecraft-server/skins");
                                outDir.mkdirs();
                                File outFile = new File(outDir, pName.toLowerCase() + "_current.png");
                                InputStream in = new URL(textureUrl).openStream();
                                FileOutputStream fos = new FileOutputStream(outFile);
                                byte[] buf = new byte[4096];
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                                fos.close();
                                in.close();
                                System.out.println("  SAVED TO: " + outFile.getAbsolutePath());

                                // Also save the value and signature for offline loading!
                                File metaFile = new File(outDir, pName.toLowerCase() + "_meta.json");
                                FileOutputStream mfos = new FileOutputStream(metaFile);
                                String metaContent = "{\n  \"player\": \"" + pName + "\",\n  \"url\": \"" + textureUrl + "\",\n  \"value\": \"" + value + "\",\n  \"signature\": \"" + (sig != null ? sig : "") + "\"\n}\n";
                                mfos.write(metaContent.getBytes("UTF-8"));
                                mfos.close();
                                System.out.println("  SAVED METADATA TO: " + metaFile.getAbsolutePath());
                            }
                        }
                    } else {
                        System.out.println("  No 'textures' property on GameProfile!");
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
