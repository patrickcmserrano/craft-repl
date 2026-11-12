package com.craft.skin;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.URL;

/**
 * Serve o perfil de um jogador offline a partir do cache local de skins,
 * sem tocar na Mojang. O cliente do SKLauncher busca o perfil por
 * sessionserver.mojang.com via HttpURLConnection e, quando a rota falha
 * (timeout IPv6), o jogador aparece com modelo genérico. Este hook responde
 * a essa busca com o JSON local.
 *
 * Chamado a partir do bytecode reescrito de com.sklauncher.injector.Utils.
 */
public final class SkinHttpHook {

    private static final String SKINS_DIR = "/home/pace/dev/minecraft/server/skins/";

    private static final String[][] PLAYERS = new String[][] {
        {"Lizzie",  "6d3fbe2dc6f5354fb12366dec0af223c", "lizzie"},
        {"Picard",  "3db477e45e873d89a8aa2f2c20c910ff", "picard"},
        {"Karlach", "7b8f0c8a2917332ab7743457dc13fede", "karlach"},
        {"pace",    "bfb04b4641a13e85b0fbbc18eccdb3c6", "pace"}
    };

    private SkinHttpHook() {}

    /**
     * Devolve o JSON do perfil se a URL for uma busca de perfil da Mojang para
     * um dos jogadores conhecidos; null caso contrario (segue para a rede).
     */
    public static String serve(URL url) {
        if (url == null) return null;
        try {
            String host = url.getHost();
            String path = url.getPath();
            System.out.println("[SkinHttpHook] readStringFromURL: " + host + (path == null ? "" : path));
            if (host == null || !host.equals("sessionserver.mojang.com")) return null;
            if (path == null) return null;
            String marker = "/session/minecraft/profile/";
            int idx = path.indexOf(marker);
            if (idx < 0) return null;

            String uuid = path.substring(idx + marker.length());
            int slash = uuid.indexOf('/');
            if (slash >= 0) uuid = uuid.substring(0, slash);
            uuid = uuid.replace("-", "").toLowerCase();
            if (uuid.isEmpty()) return null;

            for (String[] p : PLAYERS) {
                if (p[1].equals(uuid)) {
                    String json = build(p[0], p[1], p[2]);
                    if (json != null) {
                        System.out.println("[SkinHttpHook] perfil servido do cache local: " + p[0]);
                    }
                    return json;
                }
            }
            System.out.println("[SkinHttpHook] uuid nao mapeado: " + uuid);
        } catch (Throwable t) {
            // nunca derruba a chamada de rede original; cai no comportamento normal
        }
        return null;
    }

    private static String buildResponse(String playerName, String uuidNoDashes, String skinName) {
        return build(playerName, uuidNoDashes, skinName);
    }

    /** Monta o JSON de perfil a partir do arquivo local de skin; null se ausente. */
    public static String build(String playerName, String uuidNoDashes, String skinName) {
        try {
            File metaFile = new File(SKINS_DIR + skinName + "_meta.json");
            if (!metaFile.exists()) {
                metaFile = new File(SKINS_DIR + skinName + ".json");
            }
            if (!metaFile.exists()) return null;

            BufferedReader br = new BufferedReader(new FileReader(metaFile));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
            br.close();
            String json = sb.toString();

            String value = extract(json, "\"value\": \"");
            if (value == null) return null;
            String sig = extract(json, "\"signature\": \"");
            if (sig == null) sig = "";

            return "{\"id\":\"" + uuidNoDashes + "\",\"name\":\"" + playerName +
                   "\",\"properties\":[{\"name\":\"textures\",\"value\":\"" + value +
                   "\",\"signature\":\"" + sig + "\"}]}";
        } catch (Exception e) {
            return null;
        }
    }

    private static String extract(String json, String key) {
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
