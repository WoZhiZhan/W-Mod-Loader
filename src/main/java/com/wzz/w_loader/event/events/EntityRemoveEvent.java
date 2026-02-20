package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.entity.Entity;

public class EntityRemoveEvent extends Event {
    private final Object entity;
    private Entity.RemovalReason removalReason;

    public EntityRemoveEvent(Object entity, Entity.RemovalReason removalReason) {
        this.entity = entity;
        this.removalReason = removalReason;
    }

    @Override
    public boolean isCancellable() { return true; }

    public Entity getEntity() { return (Entity) entity; }

    public Entity.RemovalReason getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(Entity.RemovalReason removalReason) {
        this.removalReason = removalReason;
    }
}