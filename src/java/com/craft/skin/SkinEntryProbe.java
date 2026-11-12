package com.craft.skin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Sonda de diagnostico (lado SERVIDOR): le, na JVM viva, o GameProfile de cada
 * jogador online e imprime quantas propriedades "textures" existem, o tamanho,
 * a URL de textura e o modelo (slim/classic) decodificados do value.
 *
 * A medicao separa "a entry do servidor TEM a skin" de "a entry esta' sem skin"
 * — e e' isso que decide se o defeito esta' no servidor (nao aplicou) ou no
 * cliente (aplicou e o cliente nao renderizou).
 *
 * Sem nomes de classe do Minecraft: descobre o servidor e o player manager por
 * tipo de campo/retorno, no mesmo estilo do SkinRestorer (o classloader do
 * agente nao ve as classes do Knot, entao Class.forName("net.minecraft...")
 * estoura com ClassNotFoundException).
 *
 * Imprime via System.out (o unit minecraft-server canaliza para o journal).
 */
public final class SkinEntryProbe {

    public static void agentmain(String args, Instrumentation inst) {
        String nick = (args == null || args.isEmpty()) ? "Lizzie" : args.trim().split("\\s+")[0];
        System.out.println("[SkinEntryProbe] === alvo: " + nick + " ===");
        try {
            Object server = findServer();
            if (server == null) { System.out.println("[SkinEntryProbe] servidor nao encontrado"); return; }
            System.out.println("[SkinEntryProbe] server = " + server.getClass().getName());

            Object pm = firstZeroArg(server, "getPlayerManager", "3324");
            if (pm == null) { System.out.println("[SkinEntryProbe] playerManager nao encontrado"); return; }

            List<?> players = firstList(pm);
            if (players == null) { System.out.println("[SkinEntryProbe] lista de jogadores nao encontrada"); return; }
            System.out.println("[SkinEntryProbe] jogadores online = " + players.size());

            for (Object p : players) {
                String name = invokeString(p, "getName");
                Object profile = firstZeroArgObj(p, "getGameProfile");
                if (profile == null) { System.out.println("[SkinEntryProbe] " + name + ": sem GameProfile"); continue; }
                UUID id = (UUID) profile.getClass().getMethod("getId").invoke(profile);
                Object props = profile.getClass().getMethod("getProperties").invoke(profile);
                boolean match = nick.equalsIgnoreCase(name);
                System.out.println("[SkinEntryProbe] " + name + " uuid=" + id + (match ? "   <<< ALVO" : ""));

                Method get = props.getClass().getMethod("get", Object.class);
                Object textures = get.invoke(props, "textures");
                if (textures == null) {
                    System.out.println("[SkinEntryProbe]    textures = AUSENTE");
                    continue;
                }
                Collection<?> col = (Collection<?>) textures;
                System.out.println("[SkinEntryProbe]    textures.count = " + col.size());
                for (Object prop : col) {
                    String v = (String) prop.getClass().getMethod("value").invoke(prop);
                    String s = (String) prop.getClass().getMethod("signature").invoke(prop);
                    System.out.println("[SkinEntryProbe]    value.len = " + (v == null ? -1 : v.length()) + "  sig.len = " + (s == null ? -1 : s.length()));
                    System.out.println("[SkinEntryProbe]    decoded = " + decode(v));
                }
            }
        } catch (Throwable t) {
            System.out.println("[SkinEntryProbe] ERRO: " + t);
            t.printStackTrace(System.out);
        }
        System.out.println("[SkinEntryProbe] === fim ===");
    }

    /** Acha o servidor varrendo campos das threads de servidor, como o SkinRestorer. */
    private static Object findServer() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (!t.getName().contains("Server thread") && !t.getName().contains("VoiceChatServerThread")) continue;
            for (Field f : t.getClass().getDeclaredFields()) {
                String tn = f.getType().getName();
                if (!(tn.contains("3176") || tn.contains("1132") || tn.contains("MinecraftServer") || tn.contains("DedicatedServer"))) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(t);
                    if (v != null) return v;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static Object firstZeroArg(Object receiver, String name, String retTypeHint) throws Exception {
        for (Method m : receiver.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (name != null && m.getName().equals(name)) return m.invoke(receiver);
        }
        for (Method m : receiver.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType().getName().contains(retTypeHint)) return m.invoke(receiver);
        }
        return null;
    }

    private static Object firstZeroArgObj(Object receiver, String name) throws Exception {
        for (Method m : receiver.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getName().equals(name)) return m.invoke(receiver);
        }
        return null;
    }

    private static List<?> firstList(Object receiver) throws Exception {
        for (Method m : receiver.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                Object v = m.invoke(receiver);
                if (v != null) return (List<?>) v;
            }
        }
        return null;
    }

    private static String invokeString(Object o, String name) {
        try { return (String) o.getClass().getMethod(name).invoke(o); } catch (Throwable t) { return "?"; }
    }

    private static String decode(String value) {
        try {
            byte[] data = java.util.Base64.getDecoder().decode(value);
            String json = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            int i = json.indexOf("\"url\"");
            if (i < 0) return "(sem url) " + (json.length() > 120 ? json.substring(0, 120) : json);
            int a = json.indexOf('"', i + 5);
            int b = json.indexOf('"', a + 1);
            String url = json.substring(a + 1, b);
            String model = json.contains("slim") ? "slim" : (json.contains("default") ? "default/classic" : "?");
            return "url=" + url + " model=" + model;
        } catch (Throwable t) {
            return "(nao decodificou: " + t + ")";
        }
    }
}
