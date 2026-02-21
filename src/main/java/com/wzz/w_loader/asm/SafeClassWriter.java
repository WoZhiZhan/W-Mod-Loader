package com.wzz.w_loader.asm;

import com.wzz.w_loader.internal.library.objectweb.asm.ClassReader;
import com.wzz.w_loader.internal.library.objectweb.asm.ClassWriter;

import java.util.HashSet;
import java.util.Set;

public class SafeClassWriter extends ClassWriter {

    private final ClassLoader classLoader;

    public SafeClassWriter(ClassReader cr, int flags, ClassLoader classLoader) {
        super(cr, flags);
        this.classLoader = classLoader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) return type1;

        try {
            Set<String> supers1 = new HashSet<>();
            String s = type1;
            while (s != null) {
                supers1.add(s);
                ClassReader cr = new ClassReader(s);
                s = cr.getSuperName();
            }

            s = type2;
            while (s != null) {
                if (supers1.contains(s)) return s;
                ClassReader cr = new ClassReader(s);
                s = cr.getSuperName();
            }

        } catch (Throwable ignored) {}

        return "java/lang/Object";
    }
}
