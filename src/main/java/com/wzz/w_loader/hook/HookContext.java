package com.wzz.w_loader.hook;

import com.wzz.w_loader.event.Event;
import com.wzz.w_loader.event.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 运行时传给回调的上下文，可以取参数、取 this、取返回值
 */
public class HookContext {
    private final Object[] args;
    private final Object self;
    private final List<Event> events = new ArrayList<>();

    public HookContext(Object self, Object[] args) {
        this.self = self;
        this.args = args;
    }

    /** 发送事件并记录，方便 dispatcher 拿到 isCancelled */
    public <T extends Event> T post(T event) {
        EventBus.INSTANCE.post(event);
        events.add(event);
        return event;
    }

    public void setArg(int slot, Object value) {
        args[slot - 1] = value;
    }

    public boolean isCancelled() {
        for (Event e : events) {
            if (e.isCancellable() && e.isCancelled()) {
                return true;
            }
        }
        return false;
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @SuppressWarnings("unchecked")
    public <T> T getArg(int slot) { return (T) args[slot - 1]; }

    @SuppressWarnings("unchecked")
    public <T> T getSelf() { return (T) self; }
}