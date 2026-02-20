package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class LivingTickEvent extends Event {
    private final Object entity;

    public LivingTickEvent(Object entity) {
        this.entity = entity;
    }

    @Override
    public boolean isCancellable() { return true; }

    public LivingEntity getEntity() { return (LivingEntity) entity; }
}