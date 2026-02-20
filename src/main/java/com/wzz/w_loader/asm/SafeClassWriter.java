package com.wzz.w_loader.asm;

import com.wzz.w_loader.internal.library.objectweb.asm.ClassReader;
import com.wzz.w_loader.internal.library.objectweb.asm.ClassWriter;

/**
 * 重写 getCommonSuperClass，用指定 ClassLoader 加载类，
 * 避免 COMPUTE_FRAMES 时找不到 MC 类导致崩溃。
 */
public class SafeClassWriter extends ClassWriter {

    private final ClassLoader classLoader;

    public SafeClassWriter(ClassReader cr, int flags, ClassLoader classLoader) {
        super(cr, flags);
        this.classLoader = classLoader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        // 如果两个类型相同，直接返回
        if (type1.equals(type2)) return type1;

        try {
            Class<?> c1 = Class.forName(type1.replace('/', '.'), false, classLoader);
            Class<?> c2 = Class.forName(type2.replace('/', '.'), false, classLoader);

            if (c1.isAssignableFrom(c2)) return type1;
            if (c2.isAssignableFrom(c1)) return type2;
            if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";

            // 向上查找公共父类
            do {
                c1 = c1.getSuperclass();
            } while (!c1.isAssignableFrom(c2));

            return c1.getName().replace('.', '/');

        } catch (Exception e) {
            // 找不到类时回退到 Object，总比崩溃强
            return "java/lang/Object";
        }
    }
}