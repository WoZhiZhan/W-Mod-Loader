package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

import java.util.*;

public class CreativeTabEvent extends Event {

    private final Map<Object, List<Object>> pending = new LinkedHashMap<>();

    public void addToTab(Object tabKey, Object... items) {
        List<Object> list = pending.computeIfAbsent(tabKey, k -> new ArrayList<>());
        Collections.addAll(list, items);
    }

    public Map<Object, List<Object>> getPending() {
        return pending;
    }
}