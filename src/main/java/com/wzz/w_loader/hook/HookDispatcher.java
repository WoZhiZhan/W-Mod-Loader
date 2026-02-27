package com.wzz.w_loader.hook;

import com.wzz.w_loader.logger.WLogger;

import java.util.List;

/**
 * ASM 插入的字节码会调用这个类的静态方法。
 * 必须是静态方法，且这个类必须在 bootstrap classloader 可见。
 */
public final class HookDispatcher {

    private static final ThreadLocal<Object[]> WRITEBACK = new ThreadLocal<>();
    private static final ThreadLocal<Object>   RETURN_VALUE     = new ThreadLocal<>();
    private static final ThreadLocal<Boolean>  RETURN_VALUE_SET = new ThreadLocal<>();
    private static final ThreadLocal<Object> PRE_RETURN_VALUE = new ThreadLocal<>();

    public static boolean dispatch(String className, String methodName,
                                   String descriptor, String position,
                                   Object self, Object[] args) {
        List<HookPoint> points = HookManager.INSTANCE.getHooks(className);
        HookPoint.Position pos = HookPoint.Position.valueOf(position);
        HookContext ctx = new HookContext(self, args);
        if (pos == HookPoint.Position.TAIL) {
            ctx.initReturnValue(PRE_RETURN_VALUE.get());
            PRE_RETURN_VALUE.remove();
        }
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

        if (pos == HookPoint.Position.HEAD && !ctx.isCancelled()) {
            WRITEBACK.set(args);
        }

        if (pos == HookPoint.Position.TAIL && ctx.isReturnValueSet()) {
            RETURN_VALUE.set(ctx.getReturnValue());
            RETURN_VALUE_SET.set(Boolean.TRUE);
        }

        return ctx.isCancelled();
    }

    public static void setPreReturnValue(Object val) {
        PRE_RETURN_VALUE.set(val);
    }

    public static Object[] getAndClearWriteBack() {
        Object[] v = WRITEBACK.get();
        WRITEBACK.remove();
        return v;
    }

    public static Object getAndClearReturnValue() {
        Boolean set = RETURN_VALUE_SET.get();
        RETURN_VALUE_SET.remove();
        if (set == null || !set) return null;
        Object v = RETURN_VALUE.get();
        RETURN_VALUE.remove();
        return v;
    }
}