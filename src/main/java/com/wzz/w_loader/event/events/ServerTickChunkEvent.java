package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public class ServerTickChunkEvent extends Event {
    private final ServerLevel serverLevel;
    private final LevelChunk levelChunk;
    private final int tickSpeed;

    public ServerTickChunkEvent(ServerLevel serverLevel, LevelChunk levelChunk, int tickSpeed) {
        this.serverLevel = serverLevel;
        this.levelChunk = levelChunk;
        this.tickSpeed = tickSpeed;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public int getTickSpeed() {
        return tickSpeed;
    }

    public LevelChunk getLevelChunk() {
        return levelChunk;
    }
}
