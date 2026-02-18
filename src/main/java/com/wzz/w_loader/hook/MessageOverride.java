package com.wzz.w_loader.hook;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class MessageOverride {

    private static final ConcurrentHashMap<UUID, String> OVERRIDES = new ConcurrentHashMap<>();

    public static void set(UUID playerId, String msg) {
        OVERRIDES.put(playerId, msg);
    }

    public static String getAndClear(UUID playerId) {
        return OVERRIDES.remove(playerId);
    }

    private MessageOverride() {}
}