package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.entity.Entity;

public class EntityTickEvent extends Event {
    private final Object entity;

    public EntityTickEvent(Object entity) {
        this.entity = entity;
    }

    @Override
    public boolean isCancellable() { return true; }

    public Entity getEntity() { return (Entity) entity; }
}