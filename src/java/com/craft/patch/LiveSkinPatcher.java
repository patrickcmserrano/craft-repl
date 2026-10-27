package com.craft.patch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class LiveSkinPatcherV2 {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[LiveSkinPatcher] 🔥 Agente acoplado na JVM do Minecraft!");
        try {
            // Obter FabricLoader
            Class<?> fabricLoaderClass = null;
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (clazz.getName().equals("net.fabricmc.loader.api.FabricLoader")) {
                    fabricLoaderClass = clazz;
                    break;
                }
            }

            if (fabricLoaderClass == null) {
                System.err.println("[LiveSkinPatcher] FabricLoader não encontrado nos loaded classes!");
                return;
            }

            Object fabricInstance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object gameInstance = fabricLoaderClass.getMethod("getGameInstance").invoke(fabricInstance);

            // KnotClassLoader real é o que carregou o MinecraftClient
            ClassLoader knotLoader = gameInstance.getClass().getClassLoader();
            System.out.println("[LiveSkinPatcher] KnotClassLoader real: " + knotLoader);

            // Localizar o servidor procurando métodos sem argumentos que retornem objeto de servidor
            Object server = null;
            for (Method m : gameInstance.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    String name = m.getName();
                    String ret = m.getReturnType().getName();
                    if (name.equals("getServer") || name.equals("method_1562") || ret.contains("Server") || ret.contains("class_1132")) {
                        try {
                            Object res = m.invoke(gameInstance);
                            if (res != null) {
                                server = res;
                                System.out.println("[LiveSkinPatcher] Servidor encontrado via " + name + "(): " + res.getClass().getName());
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (server == null) {
                System.err.println("[LiveSkinPatcher] ERRO: Servidor integrado não encontrado (o mundo está aberto em LAN?)");
                return;
            }

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

            System.out.println("[LiveSkinPatcher] Buscando skin '" + fSkinName + "' para '" + fTargetPlayer + "'...");

            new Thread(() -> {
                try {
                    Class<?> skinFetcherClass = Class.forName("org.samo_lego.fabrictailor.util.SkinFetcher", true, fLoader);
                    Method fetchMethod = skinFetcherClass.getMethod("fetchSkinByName", String.class);
                    Object property = fetchMethod.invoke(null, fSkinName);

                    if (property == null) {
                        System.err.println("[LiveSkinPatcher] ❌ Não foi possível obter a skin '" + fSkinName + "'");
                        return;
                    }

                    System.out.println("[LiveSkinPatcher] ✅ Skin obtida com sucesso! Injetando no ServerThread...");

                    // Procurar método para agendar Runnable no ServerThread
                    Method runMethod = null;
                    for (Method m : fServer.getClass().getMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Runnable.class) {
                            runMethod = m;
                            break;
                        }
                    }

                    if (runMethod == null) {
                        System.err.println("[LiveSkinPatcher] Não foi possível encontrar método de agendamento Runnable no servidor!");
                        return;
                    }

                    final Method finalRunMethod = runMethod;

                    finalRunMethod.invoke(fServer, (Runnable) () -> {
                        try {
                            // Obter PlayerManager
                            Object playerManager = null;
                            for (Method m : fServer.getClass().getMethods()) {
                                if (m.getParameterCount() == 0 && (m.getName().equals("getPlayerManager") || m.getReturnType().getName().contains("PlayerManager") || m.getReturnType().getName().contains("class_3324"))) {
                                    playerManager = m.invoke(fServer);
                                    if (playerManager != null) break;
                                }
                            }

                            if (playerManager == null) {
                                System.err.println("[LiveSkinPatcher] PlayerManager não encontrado!");
                                return;
                            }

                            // Obter Player
                            Object player = null;
                            for (Method m : playerManager.getClass().getMethods()) {
                                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class && (m.getName().equals("getPlayer") || m.getReturnType().getName().contains("class_3222") || m.getReturnType().getName().contains("Player"))) {
                                    player = m.invoke(playerManager, fTargetPlayer);
                                    if (player != null) break;
                                }
                            }

                            if (player == null) {
                                System.err.println("[LiveSkinPatcher] Jogador '" + fTargetPlayer + "' não encontrado online!");
                                return;
                            }

                            System.out.println("[LiveSkinPatcher] Jogador '" + fTargetPlayer + "' localizado: " + player);

                            // Aplicar skin via FabricTailor SkinCommand
                            Class<?> skinCommandClass = Class.forName("org.samo_lego.fabrictailor.command.SkinCommand", true, fLoader);
                            for (Method m : skinCommandClass.getMethods()) {
                                if (m.getName().equals("setSkin") && m.getParameterCount() == 2) {
                                    Supplier<Object> supplier = () -> property;
                                    m.invoke(null, player, supplier);
                                    System.out.println("[LiveSkinPatcher] 🚀 SUCESSO ABSOLUTO! Skin de '" + fSkinName + "' aplicada na '" + fTargetPlayer + "' no runtime!");
                                    break;
                                }
                            }

                        } catch (Exception e) {
                            System.err.println("[LiveSkinPatcher] Erro na execução dentro do ServerThread:");
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    System.err.println("[LiveSkinPatcher] Erro no fetch da skin:");
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("[LiveSkinPatcher] Falha:");
            e.printStackTrace();
        }
    }
}
