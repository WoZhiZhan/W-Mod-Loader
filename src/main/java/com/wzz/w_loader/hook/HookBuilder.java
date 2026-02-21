package com.wzz.w_loader.hook;

public class HookBuilder {

    private final String className;
    private String methodName;
    private String descriptor = null;
    private HookPoint.Position position = HookPoint.Position.TAIL;

    // INVOKE 专用
    private String invokeOwner;
    private String invokeMethodName;
    private String invokeDesc;

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

    /**
     * 注入到方法体内某次方法调用之前。
     *
     * @param owner  被调用方法所在类（斜杠格式），null = 不限类
     * @param method 被调用方法名
     * @param desc   被调用方法描述符，null = 不限
     */
    public HookBuilder atInvoke(String owner, String method, String desc) {
        this.position = HookPoint.Position.INVOKE;
        this.invokeOwner = owner;
        this.invokeMethodName = method;
        this.invokeDesc = desc;
        return this;
    }

    public void inject(HookCallback callback) {
        HookPoint point = new HookPoint(className, methodName, descriptor, position, callback,
                                        invokeOwner, invokeMethodName, invokeDesc);
        HookManager.INSTANCE.addHook(point);
    }
}
