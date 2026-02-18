package com.wzz.w_loader.internal.transformer;

import com.wzz.w_loader.asm.SafeClassWriter;
import com.wzz.w_loader.hook.HookManager;
import com.wzz.w_loader.hook.HookPoint;
import com.wzz.w_loader.transform.IClassTransformer;
import org.objectweb.asm.*;

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
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "com/wzz/w_loader/hook/HookDispatcher", "dispatch",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                                        "Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)" +
                                        "Lcom/wzz/w_loader/event/Event;", false);
                        // stack: Event

                        if (position.equals("HEAD")) {
                            Label notCancelled = new Label();
                            Label doCancel     = new Label();

                            // ── 检查 null ──────────────────────────────────────────
                            mv.visitInsn(Opcodes.DUP);
                            // stack: Event, Event
                            mv.visitJumpInsn(Opcodes.IFNULL, notCancelled);
                            // null → notCancelled，stack 还剩: Event

                            // ── 检查 isCancellable() ───────────────────────────────
                            mv.visitInsn(Opcodes.DUP);
                            // stack: Event, Event
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                    "com/wzz/w_loader/event/Event", "isCancellable", "()Z", false);
                            // stack: Event, boolean
                            mv.visitJumpInsn(Opcodes.IFEQ, notCancelled);
                            // false → notCancelled，stack 还剩: Event

                            // ── 检查 isCancelled() ────────────────────────────────
                            mv.visitInsn(Opcodes.DUP);
                            // stack: Event, Event
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                    "com/wzz/w_loader/event/Event", "isCancelled", "()Z", false);
                            // stack: Event, boolean
                            mv.visitJumpInsn(Opcodes.IFNE, doCancel);
                            // true → doCancel，stack 还剩: Event
                            mv.visitJumpInsn(Opcodes.GOTO, notCancelled);
                            // false → notCancelled，stack 还剩: Event

                            // ── 真正取消：POP Event，插入 return ─────────────────
                            mv.visitLabel(doCancel);
                            // stack: Event
                            mv.visitInsn(Opcodes.POP);
                            // stack: {}
                            insertCancelReturn(returnType);

                            // ── 所有 notCancelled 路径：stack 都是 { Event } ─────
                            mv.visitLabel(notCancelled);
                            // stack: Event
                            mv.visitInsn(Opcodes.POP);
                            // stack: {} 统一清空

                        } else {
                            // TAIL 直接丢弃
                            mv.visitInsn(Opcodes.POP);
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