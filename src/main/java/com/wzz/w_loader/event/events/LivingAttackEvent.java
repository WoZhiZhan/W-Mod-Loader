package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingAttackEvent extends Event {
    private final Object entity;
    private final Object damageSource;
    private float damage;

    public LivingAttackEvent(Object entity, Object damageSource, float damage) {
        this.entity = entity;
        this.damageSource = damageSource;
        this.damage = damage;
    }

    @Override
    public boolean isCancellable() { return true; }

    public LivingEntity getEntity() { return (LivingEntity) entity; }
    public DamageSource getDamageSource() { return (DamageSource) damageSource; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }
}