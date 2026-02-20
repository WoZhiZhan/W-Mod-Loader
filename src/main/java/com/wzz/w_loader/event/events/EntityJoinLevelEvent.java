package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class EntityJoinLevelEvent extends Event {
    private final Object serverLevel;
    private final Object entity;

    public EntityJoinLevelEvent(Object serverLevel, Object entity) {
        this.serverLevel = serverLevel;
        this.entity = entity;
    }

    @Override
    public boolean isCancellable() { return true; }

    public Entity getEntity() { return (Entity) entity; }

    public ServerLevel getLevel() { return (ServerLevel) serverLevel; }
}
