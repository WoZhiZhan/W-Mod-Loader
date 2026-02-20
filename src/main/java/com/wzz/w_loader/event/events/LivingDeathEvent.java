package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingDeathEvent extends Event {
    private final Object entity;
    private final Object damageSource;

    public LivingDeathEvent(Object entity, Object damageSource) {
        this.entity = entity;
        this.damageSource = damageSource;
    }

    @Override
    public boolean isCancellable() { return true; }

    public LivingEntity getEntity()       { return (LivingEntity) entity; }
    public DamageSource getDamageSource() { return (DamageSource) damageSource; }
}