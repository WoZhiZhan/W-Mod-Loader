package com.wzz.w_loader.hook;

public class HookPoint {
    public enum Position { HEAD, TAIL, INVOKE }

    final String className;   // 斜杠格式
    public final String methodName;
    public final String descriptor;     // null = 匹配所有同名方法
    public final Position position;
    final HookCallback callback;

    // 仅 INVOKE 位置使用，标识目标调用点
    public final String invokeOwner;      // 被调用方法所在类（斜杠格式），null=不限
    public final String invokeMethodName; // 被调用方法名
    public final String invokeDesc;       // 被调用方法描述符，null=不限

    /** HEAD / TAIL 构造 */
    HookPoint(String className, String methodName, String descriptor,
              Position position, HookCallback callback) {
        this(className, methodName, descriptor, position, callback, null, null, null);
    }

    /** INVOKE 构造 */
    HookPoint(String className, String methodName, String descriptor,
              Position position, HookCallback callback,
              String invokeOwner, String invokeMethodName, String invokeDesc) {
        this.className       = className;
        this.methodName      = methodName;
        this.descriptor      = descriptor;
        this.position        = position;
        this.callback        = callback;
        this.invokeOwner     = invokeOwner;
        this.invokeMethodName = invokeMethodName;
        this.invokeDesc      = invokeDesc;
    }
}
