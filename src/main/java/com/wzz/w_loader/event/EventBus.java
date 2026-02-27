package com.wzz.w_loader.event;

import com.wzz.w_loader.annotation.Subscribe;
import com.wzz.w_loader.logger.WLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 注：直接调用EventBus.INSTANCE.post发布的事件无法被取消 */
public class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<? extends Event>, List<ListenerEntry>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    /** 注册某个对象里所有 @Subscribe 方法 */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) paramType;

            method.setAccessible(true);
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                     .add(new ListenerEntry(listener, method));

            WLogger.debug("Registered " + listener.getClass().getSimpleName()
                    + "#" + method.getName() + " for " + eventType.getSimpleName());
        }
    }

    public void unregister(Object listener) {
        listeners.values().forEach(list ->
            list.removeIf(entry -> entry.instance == listener));
    }

    public <T extends Event> T post(T event) {
        List<ListenerEntry> entries = listeners.get(event.getClass());
        if (entries == null) return event;

        for (ListenerEntry entry : entries) {
            if (event.isCancellable() && event.isCancelled()) break;
            try {
                entry.method.invoke(entry.instance, event);
            } catch (Exception e) {
                WLogger.error("Error in listener " + entry.method.getName());
                e.printStackTrace();
            }
        }
        return event;
    }

    public void clear() {
        listeners.clear();
    }

    private record ListenerEntry(Object instance, Method method) {}
}