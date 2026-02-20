package com.wzz.w_loader.event.events;

import net.minecraft.server.MinecraftServer;

public class ServerTickEvent extends TickEvent {
    private final MinecraftServer server;

    public ServerTickEvent(MinecraftServer server, Phase phase) {
        super(phase);
        this.server = server;
    }

    public MinecraftServer getServer() { return server; }
}