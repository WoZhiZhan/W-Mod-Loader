package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;

public class TotemTriggerEvent extends Event {
    private final Entity entity;
    private final DamageSource damageSource;

    public TotemTriggerEvent(Entity entity, DamageSource damageSource) {
        this.entity = entity;
        this.damageSource = damageSource;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (this.damageSource == null) return;
        if (this.damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            ItemStack protectionItem = null;
            DeathProtection protection;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack itemStack = living.getItemInHand(hand);
                protection = itemStack.get(DataComponents.DEATH_PROTECTION);
                if (protection != null) {
                    protectionItem = itemStack.copy();
                    break;
                }
            }
            if (protectionItem == null)
                return;
        }
        super.setCancelled(cancelled);
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public Entity getEntity() {
        return entity;
    }
}
