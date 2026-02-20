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

    void replaceCallback(String cls, String method, String descriptor,
                                HookPoint.Position pos, HookCallback newCallback) {
        List<HookPoint> points = hooks.getOrDefault(cls, Collections.emptyList());
        for (int i = 0; i < points.size(); i++) {
            HookPoint p = points.get(i);
            if (p.methodName.equals(method) && p.position == pos
                    && Objects.equals(p.descriptor, descriptor)) {
                points.set(i, new HookPoint(cls, method, descriptor, pos, newCallback));
                return;
            }
        }
    }

    public List<HookPoint> getHooks(String className) {
        return hooks.getOrDefault(className, Collections.emptyList());
    }

    public boolean hasHooks(String className) {
        return hooks.containsKey(className);
    }
}