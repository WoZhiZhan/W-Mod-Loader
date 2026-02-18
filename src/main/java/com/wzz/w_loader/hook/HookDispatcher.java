package com.wzz.w_loader.hook;

import com.wzz.w_loader.event.Event;
import com.wzz.w_loader.logger.WLogger;

import java.util.List;

/**
 * ASM 插入的字节码会调用这个类的静态方法。
 * 必须是静态方法，且这个类必须在 bootstrap classloader 可见。
 */
public final class HookDispatcher {

    private HookDispatcher() {}

    /**
     * @param className   斜杠格式类名
     * @param methodName  方法名
     * @param descriptor  方法描述符
     * @param position    "HEAD" or "TAIL"
     * @param self        this 引用（静态方法传 null）
     * @param args        方法参数数组
     * 返回 posted 的 Event 对象，调用方检查 isCancelled()
     */
    public static Event dispatch(String className, String methodName,
                                 String descriptor, String position,
                                 Object self, Object[] args) {
        List<HookPoint> points = HookManager.INSTANCE.getHooks(className);
        HookPoint.Position pos = HookPoint.Position.valueOf(position);
        Event lastEvent = null;

        for (HookPoint point : points) {
            if (!point.methodName.equals(methodName)) continue;
            if (point.descriptor != null && !point.descriptor.equals(descriptor)) continue;
            if (point.position != pos) continue;

            try {
                HookContext ctx = new HookContext(self, args);
                point.callback.call(ctx);
                if (ctx.getLastEvent() != null) lastEvent = ctx.getLastEvent();
            } catch (Throwable e) {
                WLogger.error("[HookDispatcher] " + className + "#" + methodName + " threw: " + e);
                e.printStackTrace();
            }
        }
        return lastEvent;
    }
}