package com.wzz.w_loader.hook;

public class HookBuilder {

    private final String className;
    private String methodName;
    private String descriptor = null;
    private HookPoint.Position position = HookPoint.Position.TAIL;

    HookBuilder(String className) {
        this.className = className;
    }

    public HookBuilder method(String name) {
        this.methodName = name;
        return this;
    }

    /** 精确匹配描述符，不调用则匹配所有同名方法 */
    public HookBuilder descriptor(String desc) {
        this.descriptor = desc;
        return this;
    }

    public HookBuilder atHead() {
        this.position = HookPoint.Position.HEAD;
        return this;
    }

    public HookBuilder atTail() {
        this.position = HookPoint.Position.TAIL;
        return this;
    }

    public void inject(HookCallback callback) {
        HookPoint point = new HookPoint(className, methodName, descriptor, position, callback);
        HookManager.INSTANCE.addHook(point);
    }
}