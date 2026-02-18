package com.wzz.w_loader.hook;

import com.wzz.w_loader.event.Event;
import com.wzz.w_loader.event.EventBus;

/**
 * 运行时传给回调的上下文，可以取参数、取 this、取返回值
 */
public class HookContext {
    private final Object[] args;
    private final Object self;
    private Event lastEvent;

    public HookContext(Object self, Object[] args) {
        this.self = self;
        this.args = args;
    }

    /** 发送事件并记录，方便 dispatcher 拿到 isCancelled */
    public <T extends Event> T post(T event) {
        EventBus.INSTANCE.post(event);
        this.lastEvent = event;
        return event;
    }

    public Event getLastEvent() { return lastEvent; }

    @SuppressWarnings("unchecked")
    public <T> T getArg(int slot) { return (T) args[slot - 1]; }

    @SuppressWarnings("unchecked")
    public <T> T getSelf() { return (T) self; }
}