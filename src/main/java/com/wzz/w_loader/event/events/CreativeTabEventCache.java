package com.wzz.w_loader.event.events;

import java.util.*;

public final class CreativeTabEventCache {

    private static final Map<Object, List<Object>> CACHE = new HashMap<>();

    public static void put(Object key, List<Object> items) {
        CACHE.computeIfAbsent(key, k -> new ArrayList<>()).addAll(items);
    }

    public static List<Object> get(Object key) {
        return CACHE.get(key);
    }

    private CreativeTabEventCache() {}
}