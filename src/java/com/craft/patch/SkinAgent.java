package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class SkinAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SkinAgent] Acoplado na JVM do Minecraft!");
        try {
            ClassLoader knotLoader = null;
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
                    knotLoader = clazz.getClassLoader();
                    break;
                }
            }
            if (knotLoader == null) {
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (clazz.getName().equals("net.minecraft.server.MinecraftServer")) {
                        knotLoader = clazz.getClassLoader();
                        break;
                    }
                }
            }
            if (knotLoader == null) {
                knotLoader = Thread.currentThread().getContextClassLoader();
            }

            System.out.println("[SkinAgent] KnotClassLoader localizado: " + knotLoader);

            // Carregar classes necessárias
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", true, knotLoader);
            Object fabricInstance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object gameInstance = fabricLoaderClass.getMethod("getGameInstance").invoke(fabricInstance);
            Object server = gameInstance.getClass().getMethod("getServer").invoke(gameInstance);

            if (server == null) {
                System.err.println("[SkinAgent] ERRO: Servidor não encontrado (mundo não está aberto?)");
                return;
            }

            // Parse args: "player targetSkin"
            String targetPlayer = "Lizzie";
            String skinName = "M0r7eM";
            if (args != null && !args.trim().isEmpty()) {
                String[] parts = args.trim().split("\\s+");
                if (parts.length >= 1) targetPlayer = parts[0];
                if (parts.length >= 2) skinName = parts[1];
            }

            final String fTargetPlayer = targetPlayer;
            final String fSkinName = skinName;
            final ClassLoader fLoader = knotLoader;
            final Object fServer = server;

            System.out.println("[SkinAgent] Buscando skin '" + fSkinName + "' para '" + fTargetPlayer + "'...");

            // Fetch skin em thread separada para não travar
            new Thread(() -> {
                try {
                    Class<?> skinFetcherClass = Class.forName("org.samo_lego.fabrictailor.util.SkinFetcher", true, fLoader);
                    Method fetchMethod = skinFetcherClass.getMethod("fetchSkinByName", String.class);
                    Object property = fetchMethod.invoke(null, fSkinName);

                    if (property == null) {
                        System.err.println("[SkinAgent] Não foi possível encontrar a skin '" + fSkinName + "' na Mojang/Ely.by");
                        return;
                    }

                    System.out.println("[SkinAgent] Skin obtida com sucesso! Aplicando no ServerThread...");

                    // Executar no ServerThread
                    Method executeMethod = fServer.getClass().getMethod("execute", Runnable.class);
                    executeMethod.invoke(fServer, (Runnable) () -> {
                        try {
                            Object playerManager = fServer.getClass().getMethod("getPlayerManager").invoke(fServer);
                            Object player = playerManager.getClass().getMethod("getPlayer", String.class).invoke(playerManager, fTargetPlayer);

                            if (player == null) {
                                System.err.println("[SkinAgent] Jogador '" + fTargetPlayer + "' não está online no momento!");
                                return;
                            }

                            Class<?> skinCommandClass = Class.forName("org.samo_lego.fabrictailor.command.SkinCommand", true, fLoader);
                            // setSkin(ServerPlayerEntity, Supplier<Property>)
                            for (Method m : skinCommandClass.getMethods()) {
                                if (m.getName().equals("setSkin") && m.getParameterCount() == 2) {
                                    Supplier<Object> supplier = () -> property;
                                    m.invoke(null, player, supplier);
                                    System.out.println("[SkinAgent] ✅ Skin de '" + fSkinName + "' aplicada com sucesso em '" + fTargetPlayer + "'!");
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[SkinAgent] Erro ao aplicar skin no ServerThread:");
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    System.err.println("[SkinAgent] Erro no fetch da skin:");
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("[SkinAgent] Falha geral no agente:");
            e.printStackTrace();
        }
    }
}
