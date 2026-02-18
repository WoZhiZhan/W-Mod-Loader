package com.wzz.w_loader.hook;

public class HookPoint {
    public enum Position { HEAD, TAIL }

    final String className;   // 斜杠格式
    public final String methodName;
    public final String descriptor;  // null = 匹配所有同名方法
    public final Position position;
    final HookCallback callback;

    HookPoint(String className, String methodName, String descriptor,
              Position position, HookCallback callback) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.position = position;
        this.callback = callback;
    }
}