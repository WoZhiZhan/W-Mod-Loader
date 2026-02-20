package com.wzz.w_loader.hook;

import com.wzz.w_loader.logger.WLogger;

import java.util.List;

/**
 * ASM 插入的字节码会调用这个类的静态方法。
 * 必须是静态方法，且这个类必须在 bootstrap classloader 可见。
 */
public final class HookDispatcher {

    private static final ThreadLocal<Object[]> WRITEBACK = new ThreadLocal<>();

    public static boolean dispatch(String className, String methodName,
                                   String descriptor, String position,
                                   Object self, Object[] args) {
        List<HookPoint> points = HookManager.INSTANCE.getHooks(className);
        HookPoint.Position pos = HookPoint.Position.valueOf(position);
        HookContext ctx = new HookContext(self, args);

        for (HookPoint point : points) {
            if (!point.methodName.equals(methodName)) continue;
            if (point.descriptor != null && !point.descriptor.equals(descriptor)) continue;
            if (point.position != pos) continue;
            try {
                point.callback.call(ctx);
            } catch (Throwable e) {
                WLogger.error("[HookDispatcher] " + className + "#" + methodName + " threw: " + e);
                e.printStackTrace();
            }
        }

        // HEAD 且未取消时，把 args 存起来供 transformer 写回
        if (pos == HookPoint.Position.HEAD && !ctx.isCancelled()) {
            WRITEBACK.set(args);
        }

        return ctx.isCancelled();
    }

    /** transformer 在 HEAD dispatch 之后立即调用，拿回可能被修改的 args */
    public static Object[] getAndClearWriteBack() {
        Object[] v = WRITEBACK.get();
        WRITEBACK.remove();
        return v;
    }
}