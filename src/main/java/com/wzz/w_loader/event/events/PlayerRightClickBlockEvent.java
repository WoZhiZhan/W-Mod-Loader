package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class PlayerRightClickBlockEvent extends PlayerInteractEvent {
    private Event.Result useBlock;
    private Event.Result useItem;
    private final BlockHitResult hitVec;

    public PlayerRightClickBlockEvent(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
        super(player, hand, pos, hitVec.getDirection());
        this.useBlock = Result.DEFAULT;
        this.useItem = Result.DEFAULT;
        this.hitVec = hitVec;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public Event.Result getUseBlock() {
        return this.useBlock;
    }

    public Event.Result getUseItem() {
        return this.useItem;
    }

    public BlockHitResult getHitVec() {
        return this.hitVec;
    }

    public void setUseBlock(Event.Result triggerBlock) {
        this.useBlock = triggerBlock;
    }

    public void setUseItem(Event.Result triggerItem) {
        this.useItem = triggerItem;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        super.setCancelled(cancelled);
        if (cancelled) {
            this.useBlock = Result.DENY;
            this.useItem = Result.DENY;
        }
    }
}
