package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class LivingSetHealthEvent extends Event {
    private final Object entity;
    private float value;

    public LivingSetHealthEvent(Object entity, float value) {
        this.entity = entity;
        this.value = value;
    }

    @Override
    public boolean isCancellable() { return true; }

    public LivingEntity getEntity() { return (LivingEntity) entity; }
    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }
}