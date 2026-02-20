package com.wzz.w_loader.internal.transformer;

import com.wzz.w_loader.asm.SafeClassWriter;
import com.wzz.w_loader.hook.HookManager;
import com.wzz.w_loader.hook.HookPoint;
import com.wzz.w_loader.internal.library.objectweb.asm.*;
import com.wzz.w_loader.transform.IClassTransformer;

import java.util.List;

public class UniversalTransformer implements IClassTransformer {

    private final String className;

    public UniversalTransformer(String className) {
        this.className = className;
    }

    @Override
    public String targetClass() {
        return className;
    }

    @Override
    public byte[] transform(byte[] classBytes) {
        List<HookPoint> points = HookManager.INSTANCE.getHooks(className);
        if (points.isEmpty()) return classBytes;

        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new SafeClassWriter(
                cr,
                ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
                Thread.currentThread().getContextClassLoader()
        );

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                boolean hasHead = points.stream().anyMatch(p ->
                        p.methodName.equals(name) &&
                        (p.descriptor == null || p.descriptor.equals(descriptor)) &&
                        p.position == HookPoint.Position.HEAD);

                boolean hasTail = points.stream().anyMatch(p ->
                        p.methodName.equals(name) &&
                        (p.descriptor == null || p.descriptor.equals(descriptor)) &&
                        p.position == HookPoint.Position.TAIL);

                if (!hasHead && !hasTail) return mv;

                boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                Type[] argTypes = Type.getArgumentTypes(descriptor);
                Type returnType = Type.getReturnType(descriptor);
                return new MethodVisitor(Opcodes.ASM9, mv) {

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (hasHead) {
                            insertDispatch("HEAD", isStatic, argTypes, descriptor, name, returnType);
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (hasTail && isReturnOpcode(opcode)) {
                            insertDispatch("TAIL", isStatic, argTypes, descriptor, name, returnType);
                        }
                        super.visitInsn(opcode);
                    }

                    private void insertDispatch(String position, boolean isStatic,
                                                Type[] argTypes, String desc,
                                                String methodName, Type returnType) {
                        mv.visitLdcInsn(className);
                        mv.visitLdcInsn(methodName);
                        mv.visitLdcInsn(desc);
                        mv.visitLdcInsn(position);
                        if (isStatic) mv.visitInsn(Opcodes.ACONST_NULL);
                        else          mv.visitVarInsn(Opcodes.ALOAD, 0);

                        int slot = isStatic ? 0 : 1;
                        mv.visitIntInsn(Opcodes.BIPUSH, argTypes.length);
                        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                        for (int i = 0; i < argTypes.length; i++) {
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitIntInsn(Opcodes.BIPUSH, i);
                            slot = loadArg(mv, argTypes[i], slot);
                            mv.visitInsn(Opcodes.AASTORE);
                        }
                        mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/wzz/w_loader/hook/HookDispatcher",
                                "dispatch",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                                        "Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Z",
                                false
                        );
                        // stack: Event

                        if (position.equals("HEAD")) {
                            Label notCancelled = new Label();
                            mv.visitJumpInsn(Opcodes.IFEQ, notCancelled);
                            insertCancelReturn(returnType);
                            mv.visitLabel(notCancelled);

                            // 调用 HookDispatcher.getAndClearWriteBack() 拿回 args 数组
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/wzz/w_loader/hook/HookDispatcher",
                                    "getAndClearWriteBack",
                                    "()[Ljava/lang/Object;",
                                    false);

                            // 遍历每个 primitive 参数，从 args[i] 写回对应 slot
                            int writeSlot = isStatic ? 0 : 1;
                            for (int i = 0; i < argTypes.length; i++) {
                                Type t = argTypes[i];
                                if (isPrimitive(t)) {
                                    mv.visitInsn(Opcodes.DUP);
                                    mv.visitIntInsn(Opcodes.BIPUSH, i);
                                    mv.visitInsn(Opcodes.AALOAD);
                                    unboxAndStore(mv, t, writeSlot);
                                } else {
                                    mv.visitInsn(Opcodes.DUP);
                                    mv.visitIntInsn(Opcodes.BIPUSH, i);
                                    mv.visitInsn(Opcodes.AALOAD);
                                    if (t.getSort() == Type.ARRAY) {
                                        mv.visitTypeInsn(Opcodes.CHECKCAST, t.getDescriptor());
                                    } else {
                                        mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                                    }
                                    mv.visitVarInsn(Opcodes.ASTORE, writeSlot);
                                }
                                writeSlot += t.getSize();
                            }
                            mv.visitInsn(Opcodes.POP);
                        } else {
                            // TAIL 直接丢弃
                            mv.visitInsn(Opcodes.POP);
                        }
                    }

                    private boolean isPrimitive(Type t) {
                        int s = t.getSort();
                        return s == Type.BOOLEAN || s == Type.BYTE || s == Type.CHAR ||
                                s == Type.SHORT   || s == Type.INT  || s == Type.LONG  ||
                                s == Type.FLOAT   || s == Type.DOUBLE;
                    }

                    private void unboxAndStore(MethodVisitor mv, Type type, int slot) {
                        switch (type.getSort()) {
                            case Type.BOOLEAN -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                                mv.visitVarInsn(Opcodes.ISTORE, slot); }
                            case Type.BYTE    -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                                mv.visitVarInsn(Opcodes.ISTORE, slot); }
                            case Type.CHAR    -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                                mv.visitVarInsn(Opcodes.ISTORE, slot); }
                            case Type.SHORT   -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                                mv.visitVarInsn(Opcodes.ISTORE, slot); }
                            case Type.INT     -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                                mv.visitVarInsn(Opcodes.ISTORE, slot); }
                            case Type.LONG    -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                                mv.visitVarInsn(Opcodes.LSTORE, slot); }
                            case Type.FLOAT   -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                                mv.visitVarInsn(Opcodes.FSTORE, slot); }
                            case Type.DOUBLE  -> { mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                                mv.visitVarInsn(Opcodes.DSTORE, slot); }
                        }
                    }

                    private void insertCancelReturn(Type returnType) {
                        switch (returnType.getSort()) {
                            case Type.VOID    -> mv.visitInsn(Opcodes.RETURN);
                            case Type.BOOLEAN -> { mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); }
                            case Type.INT     -> { mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); }
                            case Type.LONG    -> { mv.visitInsn(Opcodes.LCONST_0); mv.visitInsn(Opcodes.LRETURN); }
                            case Type.FLOAT   -> { mv.visitInsn(Opcodes.FCONST_0); mv.visitInsn(Opcodes.FRETURN); }
                            case Type.DOUBLE  -> { mv.visitInsn(Opcodes.DCONST_0); mv.visitInsn(Opcodes.DRETURN); }
                            default           -> { mv.visitInsn(Opcodes.ACONST_NULL); mv.visitInsn(Opcodes.ARETURN); }
                        }
                    }
                };
            }
        }, 0);

        return cw.toByteArray();
    }

    /** 加载参数到栈，处理基本类型自动装箱，返回下一个 slot */
    private int loadArg(MethodVisitor mv, Type type, int slot) {
        switch (type.getSort()) {
            case Type.BOOLEAN -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false); }
            case Type.BYTE    -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false); }
            case Type.CHAR    -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false); }
            case Type.SHORT   -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false); }
            case Type.INT     -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false); }
            case Type.LONG    -> { mv.visitVarInsn(Opcodes.LLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false); }
            case Type.FLOAT   -> { mv.visitVarInsn(Opcodes.FLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false); }
            case Type.DOUBLE  -> { mv.visitVarInsn(Opcodes.DLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false); }
            default           ->   mv.visitVarInsn(Opcodes.ALOAD, slot);
        }
        return slot + type.getSize();
    }

    private boolean isReturnOpcode(int opcode) {
        return opcode == Opcodes.RETURN  || opcode == Opcodes.ARETURN ||
               opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN ||
               opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN;
    }
}