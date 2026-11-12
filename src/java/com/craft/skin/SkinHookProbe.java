package com.craft.skin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

/**
 * Reproduz o cenario real do cliente: define o Utils transformado num
 * classloader que ve o sklauncher-fx.jar, forçando o verificador da JVM —
 * o mesmo que estoura VerifyError na retransform.
 *
 * Uso: java -cp target/bin:asm.jar com.craft.skin.SkinVerifyProbe <sklauncher.jar>
 */
public final class SkinHookProbe {

    private static final String UTILS = "com/sklauncher/injector/Utils";

    public static void main(String[] args) throws Exception {
        File jar = new File(args.length > 0 ? args[0] : "/home/pace/.minecraft/sklauncher-fx.jar");
        byte[] original = readEntry(jar, UTILS + ".class");
        if (original == null) { System.out.println("FALHA: sem " + UTILS); System.exit(1); }

        byte[] transformed = inject(original);
        System.out.println("transformado: " + transformed.length + " bytes (original " + original.length + ")");

        // classloader filho que ve o jar do cliente, mas serve o Utils transformado
        URLClassLoader parent = new URLClassLoader(new URL[]{ jar.toURI().toURL() }, null);
        ClassLoader child = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(UTILS.replace('/', '.'))) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) c = defineClass(name, transformed, 0, transformed.length);
                    if (resolve) resolveClass(c);
                    return c;
                }
                return super.loadClass(name, resolve);
            }
        };

        try {
            Class<?> utils = Class.forName(UTILS.replace('/', '.'), true, child);
            System.out.println("OK  Utils transformado carregado e verificado pela JVM");
            Method m = null;
            for (Method mm : utils.getDeclaredMethods()) {
                if (mm.getName().equals("readStringFromURL") && mm.getParameterCount() == 2) { m = mm; break; }
            }
            System.out.println(m != null ? "OK  metodo presente: " + m : "FALHA metodo ausente");
            System.out.println("== TUDO OK ==");
        } catch (VerifyError e) {
            System.out.println("FALHA VerifyError: " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            System.out.println("FALHA carga: " + t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    static byte[] inject(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected ClassLoader getClassLoader() {
                // resolve tipos comuns pelo jar do cliente anexado ao system loader
                return ClassLoader.getSystemClassLoader();
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
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/craft/skin/SkinHttpHook", "serve",
                                "(Ljava/net/URL;)Ljava/lang/String;", false);
                        mv.visitInsn(Opcodes.DUP);
                        Label skip = new Label();
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

    private static byte[] readEntry(File jar, String entry) throws Exception {
        try (ZipFile zf = new ZipFile(jar)) {
            ZipEntry e = zf.getEntry(entry);
            if (e == null) return null;
            try (java.io.InputStream in = zf.getInputStream(e)) {
                return in.readAllBytes();
            }
        }
    }
}
