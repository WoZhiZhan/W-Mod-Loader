package com.wzz.w_loader.internal.transformer;

import com.wzz.w_loader.asm.SafeClassWriter;
import com.wzz.w_loader.hook.HookManager;
import com.wzz.w_loader.hook.HookPoint;
import com.wzz.w_loader.internal.library.objectweb.asm.*;
import com.wzz.w_loader.transform.IClassTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // ── 第一遍：收集有 INVOKE hook 的方法的 maxLocals ─────────────────────
        // 必须在变换前拿到原始 maxLocals，作为临时变量 slot 的起始位置
        Map<String, Integer> methodMaxLocals = new HashMap<>();
        boolean hasAnyInvoke = points.stream()
                .anyMatch(p -> p.position == HookPoint.Position.INVOKE);

        if (hasAnyInvoke) {
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                                                 String sig, String[] ex) {
                    boolean needs = points.stream().anyMatch(p ->
                            p.position == HookPoint.Position.INVOKE &&
                            p.methodName.equals(name) &&
                            (p.descriptor == null || p.descriptor.equals(desc)));
                    if (!needs) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {
                            methodMaxLocals.put(name + desc, maxLocals);
                        }
                    };
                }
            }, 0);
        }

        // ── 第二遍：正式变换 ──────────────────────────────────────────────────
        ClassWriter cw = new SafeClassWriter(
                cr,
                ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
                Thread.currentThread().getContextClassLoader()
        );

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String sig, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);

                boolean isStatic  = (access & Opcodes.ACC_STATIC) != 0;
                Type[] argTypes   = Type.getArgumentTypes(desc);
                Type returnType   = Type.getReturnType(desc);

                boolean hasHead = points.stream().anyMatch(p ->
                        p.methodName.equals(name) &&
                        (p.descriptor == null || p.descriptor.equals(desc)) &&
                        p.position == HookPoint.Position.HEAD);

                boolean hasTail = points.stream().anyMatch(p ->
                        p.methodName.equals(name) &&
                        (p.descriptor == null || p.descriptor.equals(desc)) &&
                        p.position == HookPoint.Position.TAIL);

                boolean hasInvoke = points.stream().anyMatch(p ->
                        p.methodName.equals(name) &&
                        (p.descriptor == null || p.descriptor.equals(desc)) &&
                        p.position == HookPoint.Position.INVOKE);

                if (!hasHead && !hasTail && !hasInvoke) return mv;

                // INVOKE 注入起始 slot = 原方法 maxLocals，保证不与任何已有局部变量冲突
                int baseSlot = hasInvoke
                        ? methodMaxLocals.getOrDefault(name + desc, 64)
                        : 0;

                return new MethodVisitor(Opcodes.ASM9, mv) {

                    // ── HEAD ─────────────────────────────────────────────────
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (hasHead) {
                            insertDispatch("HEAD", isStatic, argTypes, desc, name, returnType);
                        }
                    }

                    // ── TAIL ─────────────────────────────────────────────────
                    @Override
                    public void visitInsn(int opcode) {
                        if (hasTail && isReturnOpcode(opcode)) {
                            insertDispatch("TAIL", isStatic, argTypes, desc, name, returnType);
                        }
                        super.visitInsn(opcode);
                    }

                    // ── INVOKE ────────────────────────────────────────────────
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName,
                                                String mDesc, boolean itf) {
                        if (hasInvoke) {
                            for (HookPoint point : points) {
                                if (point.position != HookPoint.Position.INVOKE) continue;
                                if (!point.methodName.equals(name)) continue;
                                if (point.descriptor != null && !point.descriptor.equals(desc)) continue;
                                if (point.invokeMethodName != null && !point.invokeMethodName.equals(mName)) continue;
                                if (point.invokeOwner     != null && !point.invokeOwner.equals(owner)) continue;
                                if (point.invokeDesc      != null && !point.invokeDesc.equals(mDesc)) continue;

                                insertInvokeDispatch(opcode, owner, mName, mDesc, itf,
                                                     isStatic, name, desc, returnType,
                                                     baseSlot);
                                return;
                            }
                        }
                        super.visitMethodInsn(opcode, owner, mName, mDesc, itf);
                    }

                    /**
                     * INVOKE 注入。
                     *
                     * 临时 slot 分配（从 baseSlot 起，baseSlot = 原方法 maxLocals）：
                     *   slot baseSlot               → receiver（虚调用时）
                     *   slot baseSlot+1 .. +N       → 被调用方法的各参数
                     *
                     * 因为 baseSlot >= 原 maxLocals，绝不与原方法任何局部变量冲突。
                     * COMPUTE_MAXS 会自动把 maxLocals 扩展到覆盖我们使用的最大 slot。
                     */
                    private void insertInvokeDispatch(int opcode, String owner, String mName,
                                                      String mDesc, boolean itf,
                                                      boolean enclosingStatic,
                                                      String enclosingName, String enclosingDesc,
                                                      Type enclosingReturn,
                                                      int base) {
                        boolean isStaticCall = (opcode == Opcodes.INVOKESTATIC);
                        Type[] invokedArgs   = Type.getArgumentTypes(mDesc);
                        Type invokedReturn   = Type.getReturnType(mDesc);

                        // ── 1. 从栈上弹出参数，按顺序保存到临时 slot ──────────
                        // 分配方案：
                        //   receiver → base
                        //   arg[0]   → base + (isStaticCall ? 0 : 1)
                        //   arg[i]   → 上一个 arg 的 slot + size
                        int receiverSlot = base;
                        int[] argSlots   = new int[invokedArgs.length];
                        {
                            int next = isStaticCall ? base : base + 1;
                            for (int i = 0; i < invokedArgs.length; i++) {
                                argSlots[i] = next;
                                next += invokedArgs[i].getSize();
                            }
                        }

                        // 栈顶是最后一个参数，逐个 STORE（倒序）
                        for (int i = invokedArgs.length - 1; i >= 0; i--) {
                            mv.visitVarInsn(invokedArgs[i].getOpcode(Opcodes.ISTORE), argSlots[i]);
                        }
                        // 保存 receiver
                        if (!isStaticCall) {
                            mv.visitVarInsn(Opcodes.ASTORE, receiverSlot);
                        }

                        // ── 2. 调用 dispatch ──────────────────────────────────
                        mv.visitLdcInsn(className);
                        mv.visitLdcInsn(enclosingName);
                        mv.visitLdcInsn(enclosingDesc);
                        mv.visitLdcInsn("INVOKE");

                        if (enclosingStatic) mv.visitInsn(Opcodes.ACONST_NULL);
                        else                  mv.visitVarInsn(Opcodes.ALOAD, 0);

                        // 构造 args 数组：[receiver?, arg0, arg1, ...]
                        int totalArgCount = invokedArgs.length + (isStaticCall ? 0 : 1);
                        pushInt(totalArgCount);
                        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

                        int arrIdx = 0;
                        if (!isStaticCall) {
                            mv.visitInsn(Opcodes.DUP);
                            pushInt(arrIdx++);
                            mv.visitVarInsn(Opcodes.ALOAD, receiverSlot);
                            mv.visitInsn(Opcodes.AASTORE);
                        }
                        for (int i = 0; i < invokedArgs.length; i++) {
                            mv.visitInsn(Opcodes.DUP);
                            pushInt(arrIdx++);
                            mv.visitVarInsn(invokedArgs[i].getOpcode(Opcodes.ILOAD), argSlots[i]);
                            box(invokedArgs[i]);
                            mv.visitInsn(Opcodes.AASTORE);
                        }

                        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "com/wzz/w_loader/hook/HookDispatcher",
                                "dispatch",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                                "Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Z",
                                false);

                        // ── 3. 判断取消 ───────────────────────────────────────
                        Label notCancelled = new Label();
                        mv.visitJumpInsn(Opcodes.IFEQ, notCancelled);
                        // 取消：压默认返回值后跳过原调用
                        pushDefaultValue(invokedReturn);
                        Label afterCall = new Label();
                        mv.visitJumpInsn(Opcodes.GOTO, afterCall);

                        // ── 4. 不取消：恢复栈并执行原调用 ────────────────────
                        mv.visitLabel(notCancelled);
                        if (!isStaticCall) {
                            mv.visitVarInsn(Opcodes.ALOAD, receiverSlot);
                        }
                        for (int i = 0; i < invokedArgs.length; i++) {
                            mv.visitVarInsn(invokedArgs[i].getOpcode(Opcodes.ILOAD), argSlots[i]);
                        }
                        mv.visitMethodInsn(opcode, owner, mName, mDesc, itf);

                        mv.visitLabel(afterCall);
                    }

                    // ── HEAD/TAIL 公用插桩（与原来完全一致）──────────────────
                    private void insertDispatch(String position, boolean isStatic,
                                                Type[] at, String desc, String mName,
                                                Type returnType) {
                        mv.visitLdcInsn(className);
                        mv.visitLdcInsn(mName);
                        mv.visitLdcInsn(desc);
                        mv.visitLdcInsn(position);
                        if (isStatic) mv.visitInsn(Opcodes.ACONST_NULL);
                        else          mv.visitVarInsn(Opcodes.ALOAD, 0);

                        int slot = isStatic ? 0 : 1;
                        pushInt(at.length);
                        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                        for (int i = 0; i < at.length; i++) {
                            mv.visitInsn(Opcodes.DUP);
                            pushInt(i);
                            slot = loadArg(at[i], slot);
                            mv.visitInsn(Opcodes.AASTORE);
                        }
                        mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/wzz/w_loader/hook/HookDispatcher",
                                "dispatch",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                                "Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Z",
                                false);

                        if (position.equals("HEAD")) {
                            Label notCancelled = new Label();
                            mv.visitJumpInsn(Opcodes.IFEQ, notCancelled);
                            insertCancelReturn(returnType);
                            mv.visitLabel(notCancelled);

                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/wzz/w_loader/hook/HookDispatcher",
                                    "getAndClearWriteBack",
                                    "()[Ljava/lang/Object;",
                                    false);

                            int writeSlot = isStatic ? 0 : 1;
                            for (int i = 0; i < at.length; i++) {
                                Type t = at[i];
                                mv.visitInsn(Opcodes.DUP);
                                pushInt(i);
                                mv.visitInsn(Opcodes.AALOAD);
                                if (isPrimitive(t)) {
                                    unboxAndStore(t, writeSlot);
                                } else {
                                    if (t.getSort() == Type.ARRAY)
                                        mv.visitTypeInsn(Opcodes.CHECKCAST, t.getDescriptor());
                                    else
                                        mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                                    mv.visitVarInsn(Opcodes.ASTORE, writeSlot);
                                }
                                writeSlot += t.getSize();
                            }
                            mv.visitInsn(Opcodes.POP);
                        } else {
                            mv.visitInsn(Opcodes.POP);
                        }
                    }

                    // ── 工具方法 ─────────────────────────────────────────────

                    private void pushInt(int value) {
                        if      (value == 0)     mv.visitInsn(Opcodes.ICONST_0);
                        else if (value == 1)     mv.visitInsn(Opcodes.ICONST_1);
                        else if (value == 2)     mv.visitInsn(Opcodes.ICONST_2);
                        else if (value == 3)     mv.visitInsn(Opcodes.ICONST_3);
                        else if (value == 4)     mv.visitInsn(Opcodes.ICONST_4);
                        else if (value == 5)     mv.visitInsn(Opcodes.ICONST_5);
                        else if (value <= 127)   mv.visitIntInsn(Opcodes.BIPUSH, value);
                        else if (value <= 32767) mv.visitIntInsn(Opcodes.SIPUSH, value);
                        else                     mv.visitLdcInsn(value);
                    }

                    private void box(Type type) {
                        switch (type.getSort()) {
                            case Type.BOOLEAN -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",   "valueOf", "(Z)Ljava/lang/Boolean;",   false);
                            case Type.BYTE    -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte",      "valueOf", "(B)Ljava/lang/Byte;",      false);
                            case Type.CHAR    -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                            case Type.SHORT   -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short",     "valueOf", "(S)Ljava/lang/Short;",     false);
                            case Type.INT     -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",   "valueOf", "(I)Ljava/lang/Integer;",   false);
                            case Type.LONG    -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long",      "valueOf", "(J)Ljava/lang/Long;",      false);
                            case Type.FLOAT   -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float",     "valueOf", "(F)Ljava/lang/Float;",     false);
                            case Type.DOUBLE  -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double",    "valueOf", "(D)Ljava/lang/Double;",    false);
                        }
                    }

                    private void pushDefaultValue(Type returnType) {
                        switch (returnType.getSort()) {
                            case Type.VOID    -> {}
                            case Type.LONG    -> mv.visitInsn(Opcodes.LCONST_0);
                            case Type.FLOAT   -> mv.visitInsn(Opcodes.FCONST_0);
                            case Type.DOUBLE  -> mv.visitInsn(Opcodes.DCONST_0);
                            case Type.BOOLEAN, Type.BYTE, Type.CHAR,
                                 Type.SHORT,  Type.INT -> mv.visitInsn(Opcodes.ICONST_0);
                            default           -> mv.visitInsn(Opcodes.ACONST_NULL);
                        }
                    }

                    private boolean isPrimitive(Type t) {
                        int s = t.getSort();
                        return s == Type.BOOLEAN || s == Type.BYTE || s == Type.CHAR ||
                               s == Type.SHORT   || s == Type.INT  || s == Type.LONG ||
                               s == Type.FLOAT   || s == Type.DOUBLE;
                    }

                    private void unboxAndStore(Type type, int slot) {
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

                    private int loadArg(Type type, int slot) {
                        switch (type.getSort()) {
                            case Type.BOOLEAN -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",   "valueOf", "(Z)Ljava/lang/Boolean;",   false); }
                            case Type.BYTE    -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte",      "valueOf", "(B)Ljava/lang/Byte;",      false); }
                            case Type.CHAR    -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false); }
                            case Type.SHORT   -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short",     "valueOf", "(S)Ljava/lang/Short;",     false); }
                            case Type.INT     -> { mv.visitVarInsn(Opcodes.ILOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",   "valueOf", "(I)Ljava/lang/Integer;",   false); }
                            case Type.LONG    -> { mv.visitVarInsn(Opcodes.LLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long",      "valueOf", "(J)Ljava/lang/Long;",      false); }
                            case Type.FLOAT   -> { mv.visitVarInsn(Opcodes.FLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float",     "valueOf", "(F)Ljava/lang/Float;",     false); }
                            case Type.DOUBLE  -> { mv.visitVarInsn(Opcodes.DLOAD, slot); mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double",    "valueOf", "(D)Ljava/lang/Double;",    false); }
                            default           ->   mv.visitVarInsn(Opcodes.ALOAD, slot);
                        }
                        return slot + type.getSize();
                    }
                };
            }
        }, 0); // 不需要 EXPAND_FRAMES，COMPUTE_FRAMES 自己处理

        return cw.toByteArray();
    }

    private boolean isReturnOpcode(int opcode) {
        return opcode == Opcodes.RETURN  || opcode == Opcodes.ARETURN ||
               opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN ||
               opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN;
    }
}
