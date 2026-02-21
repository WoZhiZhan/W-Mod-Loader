package com.wzz.w_loader.event.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import com.wzz.w_loader.event.Event;

public class ExplosionEvent extends Event {
    private final ServerLevel level;
    private final Entity source;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator explosionDamageCalculator;
    private final Level.ExplosionInteraction interactionType;
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final boolean fire;

    private boolean cancelled = false;
    private float modifiedRadius;
    private boolean modifiedFire;

    public ExplosionEvent(ServerLevel level, Entity source,
                          DamageSource damageSource, ExplosionDamageCalculator explosionDamageCalculator,
                          Level.ExplosionInteraction interactionType,
                          double x, double y, double z, float radius, boolean fire) {
        this.level = level;
        this.source = source;
        this.damageSource = damageSource;
        this.explosionDamageCalculator = explosionDamageCalculator;
        this.interactionType = interactionType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.fire = fire;

        this.modifiedRadius = radius;
        this.modifiedFire = fire;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    // Getter methods
    public ServerLevel getLevel() { return level; }
    public Entity getSource() { return source; }
    public DamageSource getDamageSource() { return damageSource; }
    public ExplosionDamageCalculator getExplosionDamageCalculator() { return explosionDamageCalculator; }
    public Level.ExplosionInteraction getInteractionType() { return interactionType; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getRadius() { return radius; }
    public boolean isFire() { return fire; }

    // Cancellation
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    // Modified values
    public float getModifiedRadius() { return modifiedRadius; }
    public void setModifiedRadius(float radius) { this.modifiedRadius = radius; }

    public boolean isModifiedFire() { return modifiedFire; }
    public void setModifiedFire(boolean fire) { this.modifiedFire = fire; }

    // Helper methods
    public boolean isRadiusModified() {
        return Math.abs(modifiedRadius - radius) > 0.001f;
    }

    public boolean isFireModified() {
        return modifiedFire != fire;
    }

    public boolean hasModifications() {
        return isRadiusModified() || isFireModified();
    }
}