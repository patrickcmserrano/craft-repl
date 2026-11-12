package com.craft.skin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Conserta a skin de jogadores offline no cliente do SKLauncher.
 *
 * O cliente busca o perfil pela sessionserver.mojang.com via
 * HttpURLConnection (MojangSkinProvider), e quando a rota falha o jogador
 * aparece com o modelo genérico. Dois alvos:
 *
 *  1. os caches de ProfileFilter/ProfileExtendedFilter (usados por quem
 *     passa pelo proxy interno do launcher);
 *  2. o metodo Utils.readStringFromURL(URL, boolean), por onde o
 *     MojangSkinProvider efetivamente pede o perfil — reescrito para
 *     devolver o JSON local antes de tocar a rede.
 */
public class ClientSkinCacheFixer {

    private static final String[] CACHE_CLASSES = new String[] {
        "com.sklauncher.injector.filters.ProfileFilter",
        "com.sklauncher.injector.filters.ProfileExtendedFilter"
    };

    private static final String UTILS_CLASS = "com.sklauncher.injector.Utils";
    private static final String HOOK_CLASS = "com/craft/skin/SkinHttpHook";

    private static final String[][] mappings = new String[][] {
        {"Lizzie", "6d3fbe2dc6f5354fb12366dec0af223c", "lizzie"},
        {"Picard", "3db477e45e873d89a8aa2f2c20c910ff", "picard"},
        {"Karlach", "7b8f0c8a2917332ab7743457dc13fede", "karlach"},
        {"pace", "bfb04b4641a13e85b0fbbc18eccdb3c6", "pace"}
    };

    public static void agentmain(String args, Instrumentation inst) {
        try {
            appendAgentJar(inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            fillProfileCaches(inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            hookUtils(inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Garante que com.craft.skin.SkinHttpHook seja visivel para o classloader
     * que carrega a Utils do launcher, que pode nao ser o system loader.
     */
    private static void appendAgentJar(Instrumentation inst) throws Exception {
        java.security.CodeSource cs = ClientSkinCacheFixer.class.getProtectionDomain().getCodeSource();
        if (cs == null) return;
        java.io.File jar = new java.io.File(cs.getLocation().toURI());
        if (jar.isFile() && jar.getName().endsWith(".jar")) {
            inst.appendToSystemClassLoaderSearch(new java.util.jar.JarFile(jar));
            System.out.println("[ClientSkinCacheFixer] agent jar no system classloader: " + jar);
        }
    }

    private static void fillProfileCaches(Instrumentation inst) throws Exception {
        for (Class<?> c : inst.getAllLoadedClasses()) {
            boolean target = false;
            for (String name : CACHE_CLASSES) {
                if (c.getName().equals(name)) target = true;
            }
            if (!target) continue;

            for (Field f : c.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(f.getType())) continue;
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
                System.out.println("[ClientSkinCacheFixer] cache atualizado em " + c.getName());
            }
        }
    }

    private static void hookUtils(Instrumentation inst) throws Exception {
        Class<?> utils = null;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals(UTILS_CLASS)) { utils = c; break; }
        }
        if (utils == null) {
            System.out.println("[ClientSkinCacheFixer] " + UTILS_CLASS + " nao carregada; nada a hookar");
            return;
        }

        final ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain pd, byte[] classfileBuffer) {
                if (!UTILS_CLASS.replace('.', '/').equals(className)) return null;
                try {
                    return injectHook(classfileBuffer, loader);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }
        };

        inst.addTransformer(transformer, true);
        try {
            inst.retransformClasses(utils);
            System.out.println("[ClientSkinCacheFixer] Utils.readStringFromURL hookado");
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    /**
     * Reescreve readStringFromURL(URL, boolean) para servir o cache local antes
     * da rede.
     *
     * O metodo alvo (Utils, class file 52) tem stack map frames, e inserir
     * codigo no topo invalida os frames originais. Por isso o writer recalcula
     * os frames (COMPUTE_FRAMES) — e a resolucao de classes comuns tem de usar
     * o classloader que carregou a Utils, nao o do ASM, senao a verificacao
     * estoura.
     */
    private static byte[] injectHook(byte[] classfileBuffer, final ClassLoader loader) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected ClassLoader getClassLoader() {
                return loader != null ? loader : super.getClassLoader();
            }
        };

        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                boolean isTarget = "readStringFromURL".equals(name)
                        && "(Ljava/net/URL;Z)Ljava/lang/String;".equals(descriptor);
                if (!isTarget) return mv;

                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        // String cached = SkinHttpHook.serve(url); if (cached != null) return cached;
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_CLASS, "serve",
                                "(Ljava/net/URL;)Ljava/lang/String;", false);
                        mv.visitInsn(Opcodes.DUP);
                        org.objectweb.asm.Label skip = new org.objectweb.asm.Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, skip);
                        mv.visitInsn(Opcodes.ARETURN);
                        mv.visitLabel(skip);
                        mv.visitInsn(Opcodes.POP);
                    }
                };
            }
        }, 0);

        return writer.toByteArray();
    }

    private static String buildResponse(String playerName, String uuidNoDashes, String skinName) {
        return SkinHttpHook.build(playerName, uuidNoDashes, skinName);
    }
}
