package com.wzz.w_loader.internal.transformer;

import com.wzz.w_loader.asm.SafeClassWriter;
import com.wzz.w_loader.internal.library.objectweb.asm.*;
import com.wzz.w_loader.transform.IClassTransformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class AccessTransformer implements IClassTransformer {

    // 解析结果
    record ATEntry(String className, String memberName, String descriptor, int targetAccess) {}

    private static final Map<String, List<ATEntry>> entries = new HashMap<>();

    public static void load(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                parse(line);
            }
        }
    }

    private static void parse(String line) {
        String[] parts = line.split("\\s+");
        int access = parseAccess(parts[0]);
        String fullTarget = parts[1];

        // 判断是类、字段还是方法
        int parenIdx = fullTarget.indexOf('(');
        int lastDot  = fullTarget.lastIndexOf('.', parenIdx == -1 ? fullTarget.length() : parenIdx);

        if (lastDot == -1) {
            // 纯类名
            entries.computeIfAbsent(fullTarget.replace('.', '/'), k -> new ArrayList<>())
                    .add(new ATEntry(fullTarget, null, null, access));
        } else {
            String className  = fullTarget.substring(0, lastDot).replace('.', '/');
            String member     = fullTarget.substring(lastDot + 1, parenIdx == -1 ? fullTarget.length() : parenIdx);
            String descriptor = parenIdx == -1 ? null : fullTarget.substring(parenIdx);
            entries.computeIfAbsent(className, k -> new ArrayList<>())
                    .add(new ATEntry(className, member, descriptor, access));
        }
    }

    private static int parseAccess(String token) {
        // "public", "protected", "public-f"（去掉 final）
        boolean removeFinal = token.endsWith("-f");
        String base = token.replace("-f", "");
        int acc = switch (base) {
            case "public"    -> Opcodes.ACC_PUBLIC;
            case "protected" -> Opcodes.ACC_PROTECTED;
            case "private"   -> Opcodes.ACC_PRIVATE;
            default          -> 0;
        };
        if (removeFinal) acc |= 0x10000; // 自定义标志位，表示要去 final
        return acc;
    }

    @Override
    public String targetClass() { return null; } // null = 对所有类生效

    @Override
    public byte[] transform(byte[] classBytes) {
        // 不会被调用，transform(String, byte[]) 覆盖了分发逻辑
        return classBytes;
    }

    @Override
    public byte[] transform(String className, byte[] classBytes) {
        List<ATEntry> atEntries = entries.get(className);
        if (atEntries == null || atEntries.isEmpty()) return classBytes;

        ClassReader cr = new ClassReader(classBytes);
        ClassWriter  cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_MAXS,
                Thread.currentThread().getContextClassLoader());

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {

            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                int newAccess = applyAccess(access, name, null, null, atEntries);
                super.visit(version, newAccess, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                int newAccess = applyAccess(access, className, name, descriptor, atEntries);
                return super.visitField(newAccess, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                int newAccess = applyAccess(access, className, name, descriptor, atEntries);
                return super.visitMethod(newAccess, name, descriptor, signature, exceptions);
            }

        }, 0);

        return cw.toByteArray();
    }

    private static int applyAccess(int original, String cls, String member,
                                   String desc, List<ATEntry> entries) {
        for (ATEntry e : entries) {
            if (member == null && e.memberName() != null) continue;
            if (member != null && !member.equals(e.memberName())) continue;
            if (desc != null && e.descriptor() != null && !desc.equals(e.descriptor())) continue;

            boolean removeFinal = (e.targetAccess() & 0x10000) != 0;
            int targetAcc = e.targetAccess() & ~0x10000;

            // 清除旧访问修饰符，写入新的
            original &= ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
            original |= targetAcc;
            if (removeFinal) original &= ~Opcodes.ACC_FINAL;
        }
        return original;
    }

    public static boolean hasEntries(String className) {
        return entries.containsKey(className);
    }

    public static Set<String> getTargetClasses() {
        return entries.keySet();
    }
}