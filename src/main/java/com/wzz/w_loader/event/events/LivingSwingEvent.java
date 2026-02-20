package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingSwingEvent extends Event {
    private final Object entity;
    private boolean sendToSwingingEntity;

    public LivingSwingEvent(Object entity, boolean sendToSwingingEntity) {
        this.entity = entity;
        this.sendToSwingingEntity = sendToSwingingEntity;
    }

    @Override
    public boolean isCancellable() { return true; }

    public LivingEntity getEntity() { return (LivingEntity) entity; }

    public boolean isSendToSwingingEntity() {
        return sendToSwingingEntity;
    }

    public void setSendToSwingingEntity(boolean sendToSwingingEntity) {
        this.sendToSwingingEntity = sendToSwingingEntity;
    }
}