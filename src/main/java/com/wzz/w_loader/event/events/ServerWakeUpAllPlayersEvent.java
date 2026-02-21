package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.server.level.ServerLevel;

public class ServerWakeUpAllPlayersEvent extends Event {
    private final ServerLevel serverLevel;

    public ServerWakeUpAllPlayersEvent(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }
}
