package com.wzz.w_loader.hook;

import com.wzz.w_loader.internal.library.objectweb.asm.*;

public class HookScanner {

    private static final String HOOK_DESC = "Lcom/wzz/w_loader/hook/Hook;";

    /**
     * premain 阶段安全调用：用 ASM 读字节码，不加载 VanillaHooks，不触碰 MC 类
     */
    public static void preRegister(Class<?>... hookClasses) {
        for (Class<?> clazz : hookClasses) {
            String resourceName = clazz.getName().replace('.', '/') + ".class";
            try (java.io.InputStream is =
                         clazz.getClassLoader().getResourceAsStream(resourceName)) {
                if (is == null) throw new RuntimeException("Cannot find: " + resourceName);
                ClassReader cr = new ClassReader(is);
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String mName,
                                                     String mDesc, String sig, String[] ex) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String annDesc, boolean visible) {
                                if (!annDesc.equals(HOOK_DESC)) return null;
                                // 读 @Hook 注解的值
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    String cls, method, descriptor = null;
                                    HookPoint.Position at = HookPoint.Position.HEAD;

                                    @Override
                                    public void visit(String name, Object value) {
                                        switch (name) {
                                            case "cls"        -> cls        = (String) value;
                                            case "method"     -> method     = (String) value;
                                            case "descriptor" -> descriptor = (String) value;
                                        }
                                    }

                                    @Override
                                    public void visitEnum(String name, String desc, String value) {
                                        if ("at".equals(name))
                                            at = HookPoint.Position.valueOf(value);
                                    }

                                    @Override
                                    public void visitEnd() {
                                        String d = (descriptor == null || descriptor.isEmpty())
                                                ? null : descriptor;
                                        // 空占位 callback，bindCallbacks 时替换
                                        HookManager.INSTANCE.addHook(
                                                new HookPoint(cls, method, d, at, ctx -> {}));
                                    }
                                };
                            }
                        };
                    }
                }, 0);
            } catch (java.io.IOException e) {
                throw new RuntimeException("preRegister failed for " + clazz, e);
            }
        }
    }

    /**
     * MC 类可用后调用，反射拿真正的 callback
     */
    public static void bindCallbacks(Class<?>... hookClasses) {
        for (Class<?> clazz : hookClasses) {
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                Hook ann = m.getAnnotation(Hook.class);
                if (ann == null) continue;
                String desc = ann.descriptor().isEmpty() ? null : ann.descriptor();
                HookManager.INSTANCE.replaceCallback(
                        ann.cls(), ann.method(), desc, ann.at(),
                        ctx -> {
                            try { m.invoke(null, ctx); }
                            catch (Exception e) { throw new RuntimeException(e); }
                        });
            }
        }
    }
}