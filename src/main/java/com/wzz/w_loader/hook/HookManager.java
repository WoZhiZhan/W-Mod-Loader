package com.wzz.w_loader.hook;

import java.util.*;

public final class HookManager {

    public static final HookManager INSTANCE = new HookManager();

    private final Map<String, List<HookPoint>> hooks = new HashMap<>();

    private HookManager() {}

    public static HookBuilder on(String className) {
        return new HookBuilder(className);
    }

    public Set<String> getHookedClasses() {
        return hooks.keySet();
    }

    void addHook(HookPoint point) {
        hooks.computeIfAbsent(point.className, k -> new ArrayList<>()).add(point);
    }

    public List<HookPoint> getHooks(String className) {
        return hooks.getOrDefault(className, Collections.emptyList());
    }

    public boolean hasHooks(String className) {
        return hooks.containsKey(className);
    }
}